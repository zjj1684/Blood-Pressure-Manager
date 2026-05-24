# Chaquopy Python 集成实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Python 信号处理算法（realtime.py）通过 Chaquopy 嵌入到 Android 应用中，使 Java 层可以直接调用 Python RealtimeProcessor 类。

**Architecture:** 使用 Chaquopy Gradle 插件在构建时打包 CPython 解释器和依赖库（numpy, scipy）。Python 代码放在 `app/src/main/python/`，Java 侧通过 Chaquopy API 调用 Python 对象。Application 启动时初始化 Python 运行时。

**Tech Stack:** Chaquopy 16.0.0, Python 3.8, numpy, scipy, AGP 8.5.2, Gradle 8.7

---

## File Structure

| 操作 | 文件路径 | 职责 |
|------|----------|------|
| Modify | `build.gradle` (root) | 添加 Chaquopy 插件声明 |
| Modify | `app/build.gradle` | 配置 Python 版本、pip 依赖、ABI 过滤器 |
| Create | `app/src/main/python/realtime.py` | Python 算法代码（从 algorithm_python/ 复制） |
| Modify | `BloodPressureApplication.java` | 初始化 Python 运行时 |
| Create | `app/src/main/java/com/bloodpressure/app/bp/PythonBpCalculator.java` | Java 包装类，调用 Python RealtimeProcessor |

---

### Task 1: 配置 Chaquopy Gradle 插件

**Files:**
- Modify: `build.gradle` (root)
- Modify: `app/build.gradle`

- [ ] **Step 1: 在 root build.gradle 添加 Chaquopy 插件**

```groovy
// build.gradle (root)
plugins {
    id 'com.android.application' version '8.5.2' apply false
    id 'com.chaquo.python' version '16.0.0' apply false
}
```

- [ ] **Step 2: 在 app/build.gradle 应用插件并配置 Python**

在 `plugins` 块中添加 `id 'com.chaquo.python'`，在 `defaultConfig` 中添加 Python 配置：

```groovy
plugins {
    id 'com.android.application'
    id 'com.chaquo.python'
}

android {
    namespace 'com.bloodpressure.app'
    compileSdk 34

    defaultConfig {
        applicationId "com.bloodpressure.app"
        minSdk 26
        targetSdk 34
        versionCode 1
        versionName "1.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters "arm64-v8a", "x86_64"
        }

        python {
            version "3.8"
            pip {
                install "numpy"
                install "scipy"
            }
        }
    }

    // ... 其余保持不变
}
```

- [ ] **Step 3: 验证 Gradle sync**

在 Android Studio 中执行 Gradle Sync，确认无报错。

Expected: Sync 成功，无 Chaquopy 相关错误。

- [ ] **Step 4: Commit**

```bash
git add build.gradle app/build.gradle
git commit -m "build: add Chaquopy plugin with numpy/scipy dependencies"
```

---

### Task 2: 放置 Python 算法代码

**Files:**
- Create: `app/src/main/python/realtime.py`

- [ ] **Step 1: 创建 Python 目录并复制算法文件**

将 `algorithm_python/realtime.py` 的内容复制到 `app/src/main/python/realtime.py`。

```bash
mkdir -p app/src/main/python
cp algorithm_python/realtime.py app/src/main/python/realtime.py
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/python/realtime.py
git commit -m "feat: add Python realtime processor for Chaquopy packaging"
```

---

### Task 3: 初始化 Python 运行时

**Files:**
- Modify: `app/src/main/java/com/bloodpressure/app/BloodPressureApplication.java`

- [ ] **Step 1: 在 Application 中初始化 Chaquopy**

```java
package com.bloodpressure.app;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class BloodPressureApplication extends Application {

    public static final String CHANNEL_ID_BLE_SERVICE = "ble_service_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        initPython();
    }

    private void initPython() {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
    }

    private void createNotificationChannels() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID_BLE_SERVICE,
                "BLE Service",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("用于 BLE 前台服务的通知");
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/bloodpressure/app/BloodPressureApplication.java
git commit -m "feat: initialize Chaquopy Python runtime on app startup"
```

---

### Task 4: 创建 Java 包装类

**Files:**
- Create: `app/src/main/java/com/bloodpressure/app/bp/PythonBpCalculator.java`

- [ ] **Step 1: 实现 PythonBpCalculator**

这个类通过 Chaquopy API 调用 Python 的 `RealtimeProcessor`，逐样本推入 ECG/PPG 数据并收集特征输出。

```java
package com.bloodpressure.app.bp;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

/**
 * 通过 Chaquopy 调用 Python RealtimeProcessor 的血压计算实现。
 * 逐样本推入 ECG/PPG，收集检测到的特征（PTT、HR 等）。
 */
public class PythonBpCalculator {

    private final PyObject processor;

    public PythonBpCalculator(int sampleRate) {
        Python py = Python.getInstance();
        PyObject module = py.getModule("realtime");
        processor = module.callAttr("RealtimeProcessor", sampleRate);
    }

    /**
     * 推入一对 ECG/PPG 样本。
     * @return 本拍检测到的特征列表（可能为空），每个元素是 Map:
     *         r_peak_idx, r_peak_amp, onset_idx, onset_amp,
     *         syspeak_idx, syspeak_amp, pulse_amp, ptt_onset, ptt_sys, hr
     */
    public java.util.List<java.util.Map<String, Object>> push(double ecg, double ppg) {
        PyObject result = processor.callAttr("push", ecg, ppg);
        return result.asList();
    }

    /**
     * 批量推入 ECG/PPG 样本数组。
     * @return 所有检测到的特征列表
     */
    public java.util.List<java.util.Map<String, Object>> processChunk(double[] ecgArr, double[] ppgArr) {
        Python py = Python.getInstance();
        PyObject np = py.getModule("numpy");
        PyObject ecgPy = np.callAttr("array", ecgArr);
        PyObject ppgPy = np.callAttr("array", ppgArr);
        PyObject result = processor.callAttr("process_chunk", ecgPy, ppgPy);
        return result.asList();
    }

    /**
     * 重置处理器状态（切换用户/重新开始测量时调用）。
     */
    public void reset() {
        // 重新创建处理器实例
        Python py = Python.getInstance();
        PyObject module = py.getModule("realtime");
        // 通过反射替换 processor 字段，或改用非 final 设计
        // 简单方案：直接新建实例
        setProcessor(module.callAttr("RealtimeProcessor", getSampleRate()));
    }

    // --- 辅助方法 ---

    private int getSampleRate() {
        return processor.get("fs").toInt();
    }

    private void setProcessor(PyObject newProcessor) {
        // 由于 processor 是 final 的，这里需要调整设计
        // 见下方说明
    }
}
```

**注意：** 上面的 `reset()` 方法需要将 `processor` 字段改为非 final。最终实现如下：

```java
package com.bloodpressure.app.bp;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import java.util.List;
import java.util.Map;

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

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> push(double ecg, double ppg) {
        PyObject result = processor.callAttr("push", ecg, ppg);
        return (List<Map<String, Object>>) result.toJava(List.class);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> processChunk(double[] ecgArr, double[] ppgArr) {
        Python py = Python.getInstance();
        PyObject np = py.getModule("numpy");
        PyObject ecgPy = np.callAttr("array", ecgArr);
        PyObject ppgPy = np.callAttr("array", ppgArr);
        PyObject result = processor.callAttr("process_chunk", ecgPy, ppgPy);
        return (List<Map<String, Object>>) result.toJava(List.class);
    }

    public void reset() {
        processor = createProcessor();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/bloodpressure/app/bp/PythonBpCalculator.java
git commit -m "feat: add PythonBpCalculator wrapper for Chaquopy RealtimeProcessor"
```

---

### Task 5: 集成验证

- [ ] **Step 1: 构建 APK 验证打包**

```bash
./gradlew assembleDebug
```

Expected: 构建成功，APK 中包含 Python 运行时和 numpy/scipy 库。

- [ ] **Step 2: 在真机或模拟器上运行验证**

安装 APK，确认 Application 启动时 Python 运行时初始化成功（无崩溃）。

- [ ] **Step 3: Commit**

```bash
git commit --allow-empty -m "chore: verify Chaquopy integration builds and runs"
```

---

## Usage Example

在需要调用算法的地方（如 BLE 数据回调中）：

```java
// 创建处理器（采样率 250Hz）
PythonBpCalculator calculator = new PythonBpCalculator(250);

// 逐样本推入
List<Map<String, Object>> features = calculator.push(ecgValue, ppgValue);
for (Map<String, Object> f : features) {
    double pttOnset = (double) f.get("ptt_onset");
    double pttSys = (double) f.get("ptt_sys");
    double hr = (double) f.get("hr");
    // ... 用 PTT 和 HR 计算血压
}

// 或批量推入
List<Map<String, Object>> allFeatures = calculator.processChunk(ecgArray, ppgArray);
```
