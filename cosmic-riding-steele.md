# 血压健康管理 Android 应用 — 设计文档与实施计划

## Context

用户需要开发一款血压健康管理 Android 应用（Java），通过 BLE 5.1 连接血压检测设备（CBN:BP01，MAC:0C84381D3010），实时采集 1000Hz 双通道 PPG/ECG 信号，显示滚动波形，并预留血压计算接口。

---

## 设计规格

### 1. 技术选型

| 项目 | 选择 |
|------|------|
| 语言 | Java |
| compileSdk | 34 (Android 14) |
| minSdk | 26 (Android 8.0 Oreo) |
| targetSdk | 34 |
| AGP | 8.5.2 |
| Gradle | 8.7 |
| JDK | 17 |
| 架构模式 | MVVM |
| UI 框架 | 传统 View + XML |
| 数据库 | Room (SQLite) |
| BLE 方案 | Android 原生 BLE API + 前台服务 |
| 波形渲染 | 自定义 View + Canvas |

**依赖库版本：**

| 库 | 版本 |
|----|------|
| Room (runtime + compiler) | 2.6.1 |
| Lifecycle/ViewModel | 2.8.7 |
| Lifecycle/LiveData | 2.8.7 |
| Material Components | 1.12.0 |
| RecyclerView | 1.3.2 |
| ConstraintLayout | 2.1.4 |

**gradle-wrapper.properties：**
```
distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
```

**build.gradle (Project)：**
```groovy
plugins {
    id 'com.android.application' version '8.5.2' apply false
}
```

**build.gradle (Module:app)：**
```groovy
android {
    compileSdk 34
    defaultConfig {
        minSdk 26
        targetSdk 34
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

dependencies {
    implementation 'androidx.room:room-runtime:2.6.1'
    annotationProcessor 'androidx.room:room-compiler:2.6.1'
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.8.7'
    implementation 'androidx.lifecycle:lifecycle-livedata:2.8.7'
    implementation 'com.google.android.material:material:1.12.0'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
}
```

### 2. BLE 通信协议

**下行指令（手机→设备）：**

| 指令 | 字节 | 说明 |
|------|------|------|
| CMD_START | `0x01` | 开始采集 |
| CMD_STOP | `0x02` | 停止采集 |

通过 BLE Characteristic（UUID: `0000FFF1-0000-1000-8000-00805F9B34FB`）Write 发送，仅 1 字节。

**上行数据（设备→手机）：**

MTU 20 字节，扣除 ATT 头 3 字节 = 17 字节可用。采用 16 字节紧凑二进制格式：

```
16 字节数据包：
┌──────┬──────┬────────────────────────────────────────────┐
│ Type │ Seq  │        5 组采样数据 (15 bytes)              │
│ 1B   │ 1B   │                                            │
└──────┴──────┴────────────────────────────────────────────┘

每组 3 字节编码一对 PPG + ECG（各 12-bit ADC）：
  Byte 0: PPG[11:4]
  Byte 1: PPG[3:0] << 4 | ECG[11:8]
  Byte 2: ECG[7:0]
```

- **Type** = `0x02`（数据包标识）
- **Seq** = 0~255 循环序号，用于检测丢包
- 每包 5 个采样点，1000Hz → 200 包/秒
- 带宽：200 × 16 = 3200 bytes/sec

**设备端需配合实现：** 按上述格式打包 PPG/ECG 数据并通过 BLE Notify 发送。

### 3. 应用架构与包结构

**MVVM 分层：**
```
View (Activity/Fragment + XML)
  ↓ 观察 LiveData
ViewModel (管理 UI 状态，协调 Service)
  ↓ 调用
Repository (数据仓库，协调本地 DB 和 BLE)
  ↓
BLE Service + Room Database
```

**包结构：**
```
com.bloodpressure.app/
├── ble/                    # BLE 通信层
│   ├── BleService.java           # 前台服务，管理 BLE 连接
│   ├── BleDataParser.java        # 解析 16 字节数据包
│   ├── BleCommandSender.java     # 发送指令（开始/停止）
│   └── BleConnectionManager.java # 连接/断开/重连管理
├── data/                   # 数据层
│   ├── db/
│   │   ├── AppDatabase.java
│   │   ├── SignalEntity.java     # 信号采样点实体
│   │   ├── SessionEntity.java    # 采集会话实体
│   │   └── SignalDao.java
│   ├── repository/
│   │   └── SignalRepository.java
│   └── model/
│       ├── SampleData.java       # 单个采样点 (ppg, ecg)
│       └── SessionInfo.java      # 采集会话信息
├── ui/                     # UI 层
│   ├── main/                     # 主页
│   ├── monitor/                  # 实时监测页
│   │   ├── MonitorFragment.java
│   │   ├── MonitorViewModel.java
│   │   └── WaveformView.java     # 自定义波形 View
│   ├── history/                  # 历史记录页
│   └── settings/                 # 设置页
├── waveform/               # 波形渲染
│   ├── WaveformRenderer.java     # Canvas 绘制逻辑
│   └── CircularBuffer.java       # 环形缓冲区
└── bp/                     # 血压计算（预留）
    └── BpCalculator.java         # 接口定义
```

### 4. BLE 服务设计

**GATT UUID 常量：**
- Service UUID: `0000FFF0-0000-1000-8000-00805F9B34FB`
- Characteristic UUID: `0000FFF1-0000-1000-8000-00805F9B34FB`（收发共用，支持 Write + Notify）

**BleService（前台服务）：**
- 继承 `Service`，启动时创建前台通知
- 管理 BLE 连接生命周期：扫描 → 连接 → 发现服务 → 订阅通知
- 通过 `Binder` 与 Activity 通信

**连接流程：**
1. 检查蓝牙权限和开启状态
2. 按设备名 "BP01" 或 MAC "0C84381D3010" 扫描
3. 连接设备 → 发现 GATT 服务
4. 协商 MTU（请求 512，实际可能只有 20）
5. 找到数据 Characteristic，启用 Notification
6. 通过 Handler 回调将解析后的数据传递给 ViewModel

**重连策略：**
- 连接断开后自动重试 3 次，间隔 2 秒
- 超过 3 次提示用户手动重连

**权限处理（Android 12+）：**
- `BLUETOOTH_SCAN`、`BLUETOOTH_CONNECT`、`BLUETOOTH_ADVERTISE`
- `ACCESS_FINE_LOCATION`
- 运行时动态申请

### 5. 波形显示设计

**WaveformView（自定义 View）：**
- 继承 `View`，重写 `onDraw()` 用 Canvas 绘制
- 上下两区域：上半 PPG（红色），下半 ECG（绿色），深色背景
- 网格线和刻度标注
- 使用 `postInvalidateDelayed(16)` 实现约 60fps 刷新

**CircularBuffer（环形缓冲区）：**
- 固定容量 5000 个采样点（5 秒数据）
- 新数据覆盖最旧数据
- `add(ppg, ecg)` 和 `getVisibleRange()` 方法
- 线程安全（BLE 线程写入，UI 线程读取）

**数据流：**
```
BLE Service → Handler → ViewModel → CircularBuffer → WaveformView.invalidate()
```

### 6. 数据库设计

**SessionEntity（采集会话）：**
```java
@Entity(tableName = "sessions")
public class SessionEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long startTime;
    public long endTime;
    public int sampleCount;
    public boolean completed;
}
```

**SignalEntity（信号数据）：**
```java
@Entity(tableName = "signals", indices = {@Index("sessionId")})
public class SignalEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long sessionId;
    public long timestamp;    // ms
    public int ppg;           // 0-4095
    public int ecg;           // 0-4095
}
```

**存储策略：** 每 50 个点（250ms）批量写入一次，使用 `@Transaction` 保证原子性。

### 7. 血压计算接口（预留）

```java
public interface BpCalculator {
    BpResult calculate(int[] ppgBuffer, int[] ecgBuffer, int sampleRate);
}

public class BpResult {
    public int systolic;    // 收缩压 mmHg
    public int diastolic;   // 舒张压 mmHg
    public int heartRate;   // 心率 bpm
}
```

### 8. 页面设计

1. **主页** — 最近测量结果，快捷进入监测
2. **实时监测页** — 上半 PPG 波形，下半 ECG 波形，底部实时数值，开始/停止按钮，连接状态
3. **历史记录页** — 采集会话列表，点击回看

导航：底部导航栏（主页 / 监测 / 历史）

---

## 实施计划

### Step 1: 项目初始化与基础架构
- 创建 Android 项目（Java，minSdk 26）
- 配置 Gradle 依赖（Room、Lifecycle/ViewModel、Material）
- 创建包结构
- 创建 Application 类
- **产出：** 可编译运行的空项目骨架

### Step 2: 数据层实现
- 定义 `SampleData`、`SessionInfo` 模型类
- 实现 `SessionEntity`、`SignalEntity` Room 实体
- 实现 `SignalDao`（insertBatch、getBySession、getAllSessions、deleteSession）
- 实现 `AppDatabase`
- 实现 `SignalRepository`
- **产出：** 完整的数据层，可通过单元测试验证

### Step 3: BLE 通信层实现
- 实现 `BleConnectionManager`（扫描、连接、MTU 协商、服务发现）
- 实现 `BleDataParser`（解析 16 字节数据包，提取 PPG/ECG 值）
- 实现 `BleCommandSender`（发送开始/停止指令）
- 实现 `BleService` 前台服务（整合连接管理、数据解析、Handler 回调）
- 处理 Android 12+ BLE 权限
- **产出：** BLE 服务可连接设备、发送指令、接收并解析数据

### Step 4: 波形渲染层实现
- 实现 `CircularBuffer`（线程安全环形缓冲区）
- 实现 `WaveformRenderer`（Canvas 绘制 PPG/ECG 波形、网格、刻度）
- 实现 `WaveformView` 自定义 View（整合渲染器和缓冲区，60fps 刷新）
- **产出：** 可实时显示滚动波形的自定义 View

### Step 5: 预留血压计算接口
- 定义 `BpCalculator` 接口
- 定义 `BpResult` 数据类
- **产出：** 接口定义完成，后续可插入具体算法实现

### Step 6: UI 页面实现
- 实现主页 Fragment（最近测量结果、导航）
- 实现实时监测 Fragment（波形 View、开始/停止按钮、状态指示、实时数值）
- 实现历史记录 Fragment（会话列表、RecyclerView）
- 实现 MainActivity（底部导航、Fragment 切换）
- 创建所有 XML 布局文件
- **产出：** 完整的三页面 UI

### Step 7: ViewModel 与集成
- 实现 `MonitorViewModel`（管理 BLE 连接状态、波形数据、实时数值）
- 连接 BleService ↔ ViewModel ↔ UI
- 实现数据流：BLE → Handler → ViewModel → CircularBuffer → WaveformView
- 实现数据持久化：采样数据批量写入 Room
- **产出：** 端到端数据流贯通，可完成一次完整的采集-显示-存储流程

### Step 8: 测试与优化
- 编写 BleDataParser 单元测试（验证数据包解析正确性）
- 编写 CircularBuffer 单元测试（验证线程安全和数据覆盖）
- 真机 BLE 连接测试
- 波形显示性能优化
- 内存和电量优化
- **产出：** 可交付的应用

---

## 验证方案

1. **编译验证：** `./gradlew assembleDebug` 成功
2. **单元测试：** BleDataParser 解析测试、CircularBuffer 测试通过
3. **BLE 连接测试：** 真机连接 BP01 设备，能发现服务、订阅通知
4. **指令测试：** 发送 CMD_START/CMD_STOP，设备正确响应
5. **数据接收测试：** 接收数据包，BleDataParser 正确提取 PPG/ECG 值
6. **波形显示测试：** 实时滚动波形流畅，无明显卡顿
7. **数据存储测试：** 采集结束后 Room 数据库中有完整的 Session 和 Signal 数据
8. **后台测试：** 切到后台，前台服务继续运行，数据持续采集
