"""实时流处理 — 小块/逐样本输入，状态保持，增量输出特征."""
import numpy as np
from collections import deque
from scipy.signal import butter, sosfilt, sosfilt_zi, bessel


class RealtimeProcessor:
    def __init__(self, fs):
        self.fs = fs
        nyq = 0.5 * fs

        # ECG 滤波器
        self.sos_ecg = butter(2, [5.0/nyq, 15.0/nyq], btype='band', output='sos')
        self.zi_ecg = sosfilt_zi(self.sos_ecg)

        # PPG 滤波器
        self.sos_bl = bessel(2, 0.5/nyq, btype='low', output='sos')
        self.zi_bl = sosfilt_zi(self.sos_bl)
        self.sos_lp = bessel(2, 10.0/nyq, btype='low', output='sos')
        self.zi_lp = sosfilt_zi(self.sos_lp)

        # 信号缓冲 (固定长度环形)
        win = int(2.0 * fs)
        self.ecg_raw = deque(maxlen=win)
        self.ecg_filt = deque(maxlen=win)
        self.ppg_raw = deque(maxlen=win)
        self.ppg_filt = deque(maxlen=win)
        self.integrated = deque(maxlen=win)

        # ECG Pan-Tompkins 中间变量
        self.diff_buf = deque(maxlen=5)
        self.sq_buf = deque(maxlen=int(0.150 * fs) + 1)

        # 检测状态
        self.idx = 0
        self.r_peaks = deque(maxlen=8)
        self.onsets = deque(maxlen=8)
        self.sys_peaks = deque(maxlen=8)
        self._last_r = -999
        self._rr_history = deque(maxlen=3)  # 最近 RR 间期 (samples)
        self._thr_ecg = 0
        self._spki = 0; self._npki = 0
        self._ecg_ready = False
        self._r_refractory = int(0.25 * fs)

    def push(self, ecg, ppg):
        """推入一对样本。返回本拍可能输出的特征 dict 列表。"""
        self.idx += 1
        i = self.idx
        return self._process(i, ecg, ppg)

    def process_chunk(self, ecg_arr, ppg_arr):
        """推入批量样本 (numpy array 或 list)。返回所有检出特征。"""
        all_results = []
        for ecg, ppg in zip(ecg_arr, ppg_arr):
            res = self.push(ecg, ppg)
            all_results.extend(res)
        return all_results

    def _process(self, i, ecg, ppg):

        # 滤波
        ef, self.zi_ecg = sosfilt(self.sos_ecg, [ecg], zi=self.zi_ecg)
        ef = ef[0]
        bl, self.zi_bl = sosfilt(self.sos_bl, [ppg], zi=self.zi_bl)
        dt = ppg - bl[0]
        pf, self.zi_lp = sosfilt(self.sos_lp, [dt], zi=self.zi_lp)
        pf = pf[0]

        # 入缓冲
        self.ecg_raw.append(ecg); self.ecg_filt.append(ef)
        self.ppg_raw.append(ppg); self.ppg_filt.append(pf)

        # --- ECG 检测 ---
        r = self._step_ecg(i, ef)

        # --- PPG 检测 ---
        o, s = self._step_ppg()

        # --- 匹配 ---
        results = []
        if r is not None:
            self.r_peaks.append(r)
        if o is not None:
            self.onsets.append(o)
        if s is not None:
            self.sys_peaks.append(s)

        # 有新 R 峰且有配套 PPG → 输出
        if r is not None and len(self.onsets) > 0 and len(self.sys_peaks) > 0:
            o_idx = self.onsets[-1]
            s_idx = self.sys_peaks[-1]
            if o_idx > r and s_idx > r:
                ptt_o = (o_idx - r) / self.fs * 1000.0
                ptt_s = (s_idx - r) / self.fs * 1000.0
                if 50 < ptt_o < 800 and 50 < ptt_s < 800:
                    # HR
                    hr = None
                    if len(self._rr_history) > 0:
                        rr_ms = np.mean(list(self._rr_history)) / self.fs * 1000.0
                        hr = 60000.0 / rr_ms
                    results.append({
                        'r_peak_idx': r,
                        'r_peak_amp': self._val(self.ecg_raw, r),
                        'onset_idx': o_idx,
                        'onset_amp': self._val(self.ppg_raw, o_idx),
                        'syspeak_idx': s_idx,
                        'syspeak_amp': self._val(self.ppg_raw, s_idx),
                        'pulse_amp': (self._val(self.ppg_raw, s_idx) -
                                      self._val(self.ppg_raw, o_idx)),
                        'ptt_onset': ptt_o, 'ptt_sys': ptt_s,
                        'hr': hr,
                    })
        if r is not None and len(self.r_peaks) >= 2:
            rlist = list(self.r_peaks)
            self._rr_history.append(rlist[-1] - rlist[-2])
        return results

    def _step_ecg(self, i, ef):
        """增量 ECG R 峰检测."""
        self.diff_buf.append(ef)
        if len(self.diff_buf) < 5:
            return None
        buf = list(self.diff_buf)
        d = (2*buf[-1] + buf[-2] - buf[-4] - 2*buf[-5]) / 8.0
        self.sq_buf.append(d ** 2)

        win = max(1, int(0.150 * self.fs))
        sq = list(self.sq_buf)
        integ = np.mean(sq[-min(win, len(sq)):])
        self.integrated.append(integ)

        # 初始化
        if not self._ecg_ready:
            if len(self.integrated) >= int(2 * self.fs):
                seg = np.array(list(self.integrated)[int(0.3*self.fs):])
                self._spki = np.max(seg) * 0.33
                self._npki = np.mean(seg) * 0.5
                self._thr_ecg = self._npki + 0.25*(self._spki - self._npki)
                self._ecg_ready = True
            return None

        if i - self._last_r < self._r_refractory:
            return None

        if integ <= self._thr_ecg:
            return None

        # 确认峰: 检查 integ 是否在下降
        ilist = list(self.integrated)
        if len(ilist) >= 4:
            if ilist[-1] < ilist[-2] and ilist[-2] >= ilist[-3]:
                peak_idx = i - 1
                # 在 raw 上回溯精修
                rlist = list(self.ecg_raw)
                back = int(0.18 * self.fs)
                lo = max(0, len(rlist) - back)
                hi = len(rlist)
                refined_raw = lo + int(np.argmax(rlist[lo:hi]))
                refined = self.idx - len(rlist) + refined_raw

                self._spki = 0.125*integ + 0.875*self._spki
                self._thr_ecg = self._npki + 0.25*(self._spki - self._npki)
                self._last_r = i
                return refined
        return None

    def _step_ppg(self):
        """增量 PPG 峰谷检测 (简化: 局部极值 + 间距约束)."""
        if len(self.ppg_filt) < 7:
            return None, None
        buf = list(self.ppg_filt)
        mid = len(buf) - 4
        dist = int(0.3 * self.fs)

        onset = syspeak = None
        # 收缩峰: 局部最大值
        if (buf[mid] > buf[mid-1] and buf[mid] >= buf[mid+1] and
            buf[mid] > buf[mid-2] and buf[mid] > buf[mid+2]):
            syspeak = self.idx - len(buf) + mid
            # 回溯找谷点
            prev = max(0, mid - dist)
            onset_local = prev + int(np.argmin(buf[prev:mid]))
            onset = self.idx - len(buf) + onset_local
        return onset, syspeak

    def _val(self, buf, idx):
        offset = self.idx - len(buf)
        pos = idx - offset
        if 0 <= pos < len(buf):
            return buf[pos]
        return np.nan
