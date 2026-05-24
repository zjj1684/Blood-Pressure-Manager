package com.bloodpressure.app.bp;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通过 Chaquopy 调用 Python RealtimeProcessor。
 * 逐样本推入 ECG/PPG，收集检测到的特征（PTT、HR 等）。
 */
public class PythonBpCalculator {

    private PyObject processor;
    private final int sampleRate;

    public PythonBpCalculator(int sampleRate) {
        this.sampleRate = sampleRate;
        this.processor = createProcessor();
    }

    private PyObject createProcessor() {
        Python py = Python.getInstance();
        PyObject module = py.getModule("realtime");
        return module.callAttr("RealtimeProcessor", sampleRate);
    }

    public List<Map<String, Object>> push(double ecg, double ppg) {
        PyObject result = processor.callAttr("push", ecg, ppg);
        return convertFeatureList(result);
    }

    public List<Map<String, Object>> processChunk(double[] ecgArr, double[] ppgArr) {
        Python py = Python.getInstance();
        PyObject np = py.getModule("numpy");
        PyObject ecgPy = np.callAttr("array", ecgArr);
        PyObject ppgPy = np.callAttr("array", ppgArr);
        PyObject result = processor.callAttr("process_chunk", ecgPy, ppgPy);
        return convertFeatureList(result);
    }

    private List<Map<String, Object>> convertFeatureList(PyObject pyList) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (PyObject item : pyList.asList()) {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<PyObject, PyObject> entry : item.asMap().entrySet()) {
                map.put(entry.getKey().toString(), entry.getValue().toJava(Object.class));
            }
            out.add(map);
        }
        return out;
    }

    public void reset() {
        processor = createProcessor();
    }
}
