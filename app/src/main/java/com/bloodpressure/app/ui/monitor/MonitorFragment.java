package com.bloodpressure.app.ui.monitor;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bloodpressure.app.R;
import com.bloodpressure.app.ble.BleConnectionManager;
import com.bloodpressure.app.ble.BleService;
import com.bloodpressure.app.bp.PythonBpCalculator;
import com.bloodpressure.app.data.model.SampleData;
import com.bloodpressure.app.ui.main.SharedBleViewModel;
import com.bloodpressure.app.waveform.WaveformView;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MonitorFragment extends Fragment {

    private static final int REQUEST_BLE_PERMISSIONS = 1001;

    private SharedBleViewModel sharedViewModel;
    private WaveformView waveformView;
    private PythonBpCalculator calculator;
    private int totalSamplesSent = 0;
    private static final int ALGO_BATCH_SIZE = 1000;
    private final List<Double> ecgBuffer = new ArrayList<>();
    private final List<Double> ppgBuffer = new ArrayList<>();
    private final ExecutorService algoExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextView tvStatus;
    private TextView tvSystolic;
    private TextView tvDiastolic;
    private TextView tvHeartRate;
    private MaterialButton btnConnect;
    private MaterialButton btnStartStop;
    private View statusDot;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_monitor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedBleViewModel.class);

        waveformView = view.findViewById(R.id.waveform_view);
        tvStatus = view.findViewById(R.id.tv_status);
        tvSystolic = view.findViewById(R.id.tv_systolic);
        tvDiastolic = view.findViewById(R.id.tv_diastolic);
        tvHeartRate = view.findViewById(R.id.tv_heart_rate);
        btnConnect = view.findViewById(R.id.btn_connect);
        btnStartStop = view.findViewById(R.id.btn_start_stop);
        statusDot = view.findViewById(R.id.view_status_dot);

        btnConnect.setOnClickListener(v -> onConnectClicked());
        btnStartStop.setOnClickListener(v -> onStartStopClicked());

        sharedViewModel.getConnectionState().observe(getViewLifecycleOwner(), state -> {
            tvStatus.setText(getStateText(state));
            boolean connected = state == BleConnectionManager.State.CONNECTED;
            btnStartStop.setEnabled(connected);
            btnConnect.setText(connected ? R.string.btn_disconnect : R.string.btn_connect);
            statusDot.setBackgroundResource(connected
                    ? R.drawable.bg_status_dot_connected
                    : R.drawable.bg_status_dot_disconnected);
            if (!connected) {
                btnStartStop.setText(R.string.btn_start);
            }
        });

        sharedViewModel.getSystolic().observe(getViewLifecycleOwner(), sys -> {
            tvSystolic.setText(sys > 0 ? String.valueOf(sys) : "--");
        });

        sharedViewModel.getDiastolic().observe(getViewLifecycleOwner(), dia -> {
            tvDiastolic.setText(dia > 0 ? String.valueOf(dia) : "--");
        });

        sharedViewModel.getHeartRate().observe(getViewLifecycleOwner(), hr -> {
            tvHeartRate.setText(hr > 0 ? hr + " bpm" : "-- bpm");
        });

        sharedViewModel.getBleServiceLiveData().observe(getViewLifecycleOwner(), service -> {
            if (service != null) {
                setupBleCallbacks(service);
                // 同步采集状态到按钮
                if (service.isCollecting()) {
                    btnStartStop.setText(R.string.btn_stop);
                }
            }
        });
    }

    private void setupBleCallbacks(BleService service) {
        service.setDataCallback(new BleService.DataCallback() {
            @Override
            public void onSamplesReceived(List<SampleData> samples) {
                waveformView.addBatch(samples);

                if (calculator == null || samples.isEmpty()) return;

                for (SampleData s : samples) {
                    ecgBuffer.add((double) s.getEcg());
                    ppgBuffer.add((double) s.getPpg());
                }

                if (ecgBuffer.size() >= ALGO_BATCH_SIZE) {
                    double[] ecgArr = new double[ALGO_BATCH_SIZE];
                    double[] ppgArr = new double[ALGO_BATCH_SIZE];
                    for (int i = 0; i < ALGO_BATCH_SIZE; i++) {
                        ecgArr[i] = ecgBuffer.remove(0);
                        ppgArr[i] = ppgBuffer.remove(0);
                    }

                    algoExecutor.execute(() -> {
                        List<Map<String, Object>> features = calculator.processChunk(ecgArr, ppgArr);
                        totalSamplesSent += ALGO_BATCH_SIZE;
                        Log.d("BpCalc", "samples_sent=" + totalSamplesSent
                                + " features=" + features.size());

                        for (Map<String, Object> f : features) {
                            Object hrObj = f.get("hr");
                            if (hrObj instanceof Number) {
                                double hrVal = ((Number) hrObj).doubleValue();
                                if (!Double.isNaN(hrVal)) {
                                    int hr = (int) Math.round(hrVal);
                                    mainHandler.post(() -> sharedViewModel.updateBpValues(0, 0, hr));
                                }
                            }
                        }
                    });
                }
            }

            @Override
            public void onConnectionStateChanged(BleConnectionManager.State state) {
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void onConnectClicked() {
        if (!checkBlePermissions()) return;

        BleService service = sharedViewModel.getBleService();
        if (service == null) {
            Toast.makeText(requireContext(), "服务未绑定，请稍后重试", Toast.LENGTH_SHORT).show();
            return;
        }
        if (service.getConnectionState() == BleConnectionManager.State.CONNECTED) {
            service.disconnect();
            Toast.makeText(requireContext(), "已断开连接", Toast.LENGTH_SHORT).show();
        } else {
            service.startScan();
            Toast.makeText(requireContext(), "开始连接...", Toast.LENGTH_SHORT).show();
        }
    }

    private void onStartStopClicked() {
        BleService service = sharedViewModel.getBleService();
        if (service == null) {
            Toast.makeText(requireContext(), "服务未绑定", Toast.LENGTH_SHORT).show();
            return;
        }
        if (service.isCollecting()) {
            service.stopCollecting();
            calculator = null;
            btnStartStop.setText(R.string.btn_start);
            Toast.makeText(requireContext(), "已发送停止指令 (0x02)", Toast.LENGTH_SHORT).show();
        } else {
            long sessionId = System.currentTimeMillis();
            boolean ok = service.startCollecting(sessionId);
            if (ok) {
                calculator = new PythonBpCalculator(1000);
                totalSamplesSent = 0;
                ecgBuffer.clear();
                ppgBuffer.clear();
                btnStartStop.setText(R.string.btn_stop);
                waveformView.clear();
                Toast.makeText(requireContext(), "发送成功 (0x01)", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "发送失败!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean checkBlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] permissions = {
                    "android.permission.BLUETOOTH_SCAN",
                    "android.permission.BLUETOOTH_CONNECT",
                    "android.permission.ACCESS_FINE_LOCATION"
            };

            boolean allGranted = true;
            for (String perm : permissions) {
                if (ContextCompat.checkSelfPermission(requireContext(), perm) != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                }
            }

            if (!allGranted) {
                requestPermissions(permissions, REQUEST_BLE_PERMISSIONS);
                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), "android.permission.ACCESS_FINE_LOCATION")
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION"}, REQUEST_BLE_PERMISSIONS);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLE_PERMISSIONS) {
            boolean allGranted = grantResults.length > 0;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                onConnectClicked();
            } else {
                Toast.makeText(requireContext(), "需要蓝牙和位置权限才能扫描设备", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        BleService service = sharedViewModel.getBleService();
        if (service != null) {
            service.setDataCallback(null);
        }
        algoExecutor.shutdownNow();
    }

    private String getStateText(BleConnectionManager.State state) {
        if (state == null) return getString(R.string.status_disconnected);
        switch (state) {
            case SCANNING: return getString(R.string.status_connecting);
            case CONNECTING: return getString(R.string.status_connecting);
            case CONNECTED: return getString(R.string.status_connected);
            default: return getString(R.string.status_disconnected);
        }
    }
}
