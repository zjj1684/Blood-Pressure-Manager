# BLE 通信协议规范

## 1. 概述

| 项目 | 说明 |
|------|------|
| 通信方式 | BLE 5.1 (Bluetooth Low Energy) |
| 主机 | 手机端 Android App |
| 从机 | 血压检测设备 CBN:BP01 (MAC: 0C84381D3010) |
| GATT Service UUID | `0000FFF0-0000-1000-8000-00805F9B34FB` |
| GATT Characteristic UUID | `0000FFF1-0000-1000-8000-00805F9B34FB` |
| 特征属性 | Write (下行指令) + Notify (上行数据) |
| MTU | 20 字节（ATT 头 3 字节，有效载荷 17 字节） |
| UART 波特率 | 921600 bps（MCU ↔ BLE 模组） |

---

## 2. 连接流程

```
主机 (App)                        从机 (BP01)
    |                                 |
    |--- BLE Scan (设备名/MA C) ----->|
    |<-- Scan Response ---------------|
    |                                 |
    |--- Connect Request ------------>|
    |<-- Connection Complete ---------|
    |                                 |
    |--- Discover Services ---------->|
    |<-- Service Discovery Complete --|
    |                                 |
    |--- Enable Notification -------->|
    |<-- Notification Enabled --------|
    |                                 |
    |    (连接建立，等待指令)          |
```

---

## 3. 下行指令（主机 → 从机）

主机通过 BLE Write 向 Characteristic `0000FFF1-...` 发送 **1 字节** 指令。

| 指令名称 | 字节值 | 功能 | 说明 |
|----------|--------|------|------|
| CMD_START | `0x01` | 开始采集 | 设备收到后开始以 1000Hz 采样并 Notify 上报 |
| CMD_STOP  | `0x02` | 停止采集 | 设备收到后停止采样和数据上报 |

### 发送方式

```
Write Type: WRITE_TYPE_NO_RESPONSE (Write without response)
Write Value: [0x01] 或 [0x02]
```

---

## 4. 上行数据（从机 → 主机）

从机通过 BLE Notify 向主机推送采样数据包，每包 **5 字节**，包含 **1 组**采样数据。

### 4.1 数据包格式

```
偏移    长度    字段       说明
──────────────────────────────────────────────
0x00    1B      Type       数据包类型标识 (固定 0x02)
0x01    1B      Seq        序号 (0~255 循环，用于丢包检测)
0x02    3B      Data       1 组采样数据 (PPG + ECG)
──────────────────────────────────────────────
总计    5B
```

### 4.2 数据包结构图

```
Byte:  [0]     [1]     [2]     [3]     [4]
       ┌─────┬─────┬─────────────────────────┐
       │Type │ Seq │      Sample 0 (3B)      │
       │ 1B  │ 1B  │  PPG[11:0] + ECG[11:0] │
       └─────┴─────┴─────────────────────────┘
             │
             ├── Type = 0x02 (数据包标识)
             └── Seq  = 0x00 ~ 0xFF (循环递增)
```

### 4.3 采样数据编码（每组 3 字节）

每个采样点包含一对 PPG + ECG 值，各 **12-bit ADC**（范围 0~4095），紧凑编码为 3 字节：

```
Byte 0:  PPG[11:4]              — PPG 高 8 位
Byte 1:  PPG[3:0] << 4 | ECG[11:8]  — PPG 低 4 位 + ECG 高 4 位
Byte 2:  ECG[7:0]               — ECG 低 8 位
```

#### 位域详解

```
Byte 0:  [P11] [P10] [P9] [P8] [P7] [P6] [P5] [P4]
Byte 1:  [P3]  [P2]  [P1] [P0] [E11][E10][E9] [E8]
Byte 2:  [E7]  [E6]  [E5] [E4] [E3] [E2] [E1] [E0]
```

#### 解码公式

```
PPG = (Byte0 << 4) | (Byte1 >> 4)
ECG = ((Byte1 & 0x0F) << 8) | Byte2
```

#### 解码示例

```
收到字节: 0x80 0x04 0x00

PPG = (0x80 << 4) | (0x04 >> 4)
    = 0x800 | 0x000
    = 2048 (0x800)

ECG = ((0x04 & 0x0F) << 8) | 0x00
    = (0x04 << 8) | 0x00
    = 1024 (0x400)
```

### 4.4 序号与丢包检测

- `Seq` 字段范围 `0x00` ~ `0xFF`（0~255），每包递增 1，溢出后回绕到 0
- 主机收到包后检查 `Seq` 是否为 `上一包 Seq + 1`（模 256）
- 若不连续，说明中间有丢包

```
正常:  Seq=0x00 → Seq=0x01 → Seq=0x02 → ...
丢包:  Seq=0x00 → Seq=0x03  (丢失 0x01, 0x02)
```

---

## 5. 时序参数

| 参数 | 值 | 说明 |
|------|-----|------|
| 采样率 | 1000 Hz | 每秒 1000 个采样点 |
| 每包采样数 | 1 | 每包包含 1 组 PPG+ECG |
| 发包频率 | 1000 包/秒 | 1000 / 1 = 1000 |
| 单包大小 | 5 字节 | 1B Type + 1B Seq + 3B Data |
| 有效数据 | 4 字节/包 | 扣除 Type 后的有效载荷 |
| 带宽 | ~5000 bytes/sec | 1000 × 5 = 5000 |
| UART 波特率 | 921600 bps | MCU ↔ BLE 模组通信速率 |

---

## 6. 完整通信时序

```
主机 (App)                           从机 (BP01)
    |                                    |
    |==== 连接阶段 =====================|
    |                                    |
    |--- BLE Connect ------------------>|
    |--- Discover Services ------------>|
    |--- Enable Notification ---------->|
    |                                    |
    |==== 采集阶段 =====================|
    |                                    |
    |--- CMD_START (0x01) ------------->|
    |                                    |
    |<-- Data Packet (Seq=0x00) --------|  ← 1 个采样点
    |<-- Data Packet (Seq=0x01) --------|  ← 1 个采样点
    |<-- Data Packet (Seq=0x02) --------|  ← 1 个采样点
    |       ... (1000 包/秒) ...         |
    |                                    |
    |==== 停止阶段 =====================|
    |                                    |
    |--- CMD_STOP (0x02) -------------->|
    |                                    |
    |    (设备停止采样和上报)            |
```

---

## 7. Android 端参考实现

### 7.1 数据解析

```java
// 解码单个采样点 (3 bytes → PPG + ECG)
int ppg = ((byte0 & 0xFF) << 4) | ((byte1 & 0xFF) >> 4);
int ecg = (((byte1 & 0x0F) << 8) | (byte2 & 0xFF));
```

### 7.2 发送指令

```java
characteristic.setValue(new byte[]{0x01}); // CMD_START
characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
gatt.writeCharacteristic(characteristic);
```

---

## 8. 项目代码对应关系

| 协议功能 | 文件路径 | 关键代码 |
|----------|----------|----------|
| **GATT UUID 定义** | [BleCommandSender.java](app/src/main/java/com/bloodpressure/app/ble/BleCommandSender.java) | `SERVICE_UUID = "0000FFF0-..."`, `CHARACTERISTIC_UUID = "0000FFF1-..."` |
| **下行指令定义** | [BleCommandSender.java](app/src/main/java/com/bloodpressure/app/ble/BleCommandSender.java) | `CMD_START = 0x01`, `CMD_STOP = 0x02` |
| **指令发送** | [BleCommandSender.java](app/src/main/java/com/bloodpressure/app/ble/BleCommandSender.java) | `sendStartCommand()`, `sendStopCommand()` |
| **上行数据解析** | [BleDataParser.java](app/src/main/java/com/bloodpressure/app/ble/BleDataParser.java) | `parse(byte[] data)` — 解析 Type、Seq、提取 PPG/ECG |
| **丢包检测** | [BleDataParser.java](app/src/main/java/com/bloodpressure/app/ble/BleDataParser.java) | `lastSeq` 追踪 + `droppedPackets` 计数 |
| **BLE 扫描与连接** | [BleConnectionManager.java](app/src/main/java/com/bloodpressure/app/ble/BleConnectionManager.java) | `startScan()`, `connectToDevice()`, GATT 回调 |
| **服务发现与通知订阅** | [BleConnectionManager.java](app/src/main/java/com/bloodpressure/app/ble/BleConnectionManager.java) | `onServicesDiscovered()`, `enableNotification()` |
| **BLE 前台服务** | [BleService.java](app/src/main/java/com/bloodpressure/app/ble/BleService.java) | 整合连接管理、数据解析、状态回调 |
| **采样数据模型** | [SampleData.java](app/src/main/java/com/bloodpressure/app/data/model/SampleData.java) | `ppg`, `ecg` 字段 |
| **数据包单元测试** | [BleDataParserTest.java](app/src/test/java/com/bloodpressure/app/BleDataParserTest.java) | 验证解码、Seq 追踪、边界条件 |
| **协议常量定义** | [BLE-Protocol.md](BLE-Protocol.md) | 本文档 |
