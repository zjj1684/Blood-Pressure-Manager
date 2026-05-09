package com.bloodpressure.app.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.UUID;

public class BleConnectionManager {

    private static final String TAG = "BleConnectionManager";
    private static final String TARGET_DEVICE_NAME = "BP01";
    private static final String TARGET_MAC = "10:30:1D:38:84:0C";
    private static final UUID CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");
    private static final long SCAN_TIMEOUT = 15000;
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private static final long RECONNECT_DELAY = 2000;

    public enum State { IDLE, SCANNING, CONNECTING, CONNECTED, DISCONNECTED }

    public interface Callback {
        void onStateChanged(State state);
        void onDataReceived(byte[] data);
        void onError(String message);
    }

    private final Context context;
    private final Handler mainHandler;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothGatt bluetoothGatt;
    private BleCommandSender commandSender;
    private Callback callback;
    private State state = State.IDLE;
    private int reconnectAttempts = 0;
    private boolean userDisconnected = false;

    public BleConnectionManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = manager != null ? manager.getAdapter() : null;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public State getState() {
        return state;
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public void startScan() {
        if (bluetoothAdapter == null) {
            notifyError("蓝牙不可用");
            return;
        }

        // Android 12+ 需要位置服务开启才能扫描到 BLE 设备
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null
                    && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                notifyError("请开启位置服务以扫描蓝牙设备");
                return;
            }
        }

        // 先尝试直接连接（已知 MAC 地址，无需扫描）
        connectDirect();
    }

    private void connectDirect() {
        if (state == State.CONNECTING || state == State.SCANNING) return;

        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(TARGET_MAC);
            setState(State.CONNECTING);
            reconnectAttempts = 0;
            userDisconnected = false;
            bluetoothGatt = device.connectGatt(context, false, gattCallback);

            mainHandler.postDelayed(() -> {
                if (state == State.CONNECTING) {
                    if (bluetoothGatt != null) {
                        bluetoothGatt.close();
                        bluetoothGatt = null;
                    }
                    startBleScan();
                }
            }, 10000);
        } catch (SecurityException e) {
            notifyError("蓝牙权限不足，请授予蓝牙连接权限");
            setState(State.IDLE);
        } catch (Exception e) {
            notifyError("连接失败: " + e.getMessage());
            setState(State.IDLE);
        }
    }

    private void startBleScan() {
        if (state == State.SCANNING) return;

        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bleScanner == null) {
            notifyError("无法获取 BLE 扫描器");
            setState(State.IDLE);
            return;
        }

        try {
            bleScanner.stopScan(scanCallback);
        } catch (Exception ignored) {
        }

        setState(State.SCANNING);
        bleScanner.startScan(scanCallback);

        mainHandler.postDelayed(() -> {
            if (state == State.SCANNING) {
                bleScanner.stopScan(scanCallback);
                notifyError("扫描超时，未找到设备，请确认设备已开机并在附近");
                setState(State.IDLE);
            }
        }, SCAN_TIMEOUT);
    }

    public void stopScan() {
        if (bleScanner != null) {
            try {
                bleScanner.stopScan(scanCallback);
            } catch (Exception ignored) {
            }
        }
        setState(State.IDLE);
    }

    public void disconnect() {
        userDisconnected = true;
        reconnectAttempts = MAX_RECONNECT_ATTEMPTS;
        stopScan();
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
        }
    }

    public BleCommandSender getCommandSender() {
        return commandSender;
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            try {
                BluetoothDevice device = result.getDevice();
                String name = device.getName();
                String address = device.getAddress();

                if (TARGET_DEVICE_NAME.equals(name) || TARGET_MAC.equalsIgnoreCase(address)) {
                    bleScanner.stopScan(this);
                    connectToDevice(device);
                }
            } catch (SecurityException e) {
                notifyError("蓝牙权限不足，请授予蓝牙权限");
                bleScanner.stopScan(this);
                setState(State.IDLE);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            String message;
            switch (errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    message = "扫描已在进行中，正在重置...";
                    break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    message = "扫描注册失败，请检查蓝牙权限";
                    break;
                case SCAN_FAILED_FEATURE_UNSUPPORTED:
                    message = "设备不支持 BLE 扫描";
                    break;
                case SCAN_FAILED_INTERNAL_ERROR:
                    message = "扫描内部错误";
                    break;
                default:
                    message = "扫描失败，错误码: " + errorCode;
                    break;
            }
            Log.e(TAG, "onScanFailed: " + message + " (code=" + errorCode + ")");

            if (bleScanner != null) {
                try {
                    bleScanner.stopScan(this);
                } catch (Exception ignored) {
                }
            }

            if (errorCode == SCAN_FAILED_ALREADY_STARTED && bleScanner != null) {
                mainHandler.postDelayed(() -> {
                    if (state == State.SCANNING) {
                        try {
                            bleScanner.startScan(scanCallback);
                        } catch (Exception e) {
                            notifyError("扫描重试失败");
                            setState(State.IDLE);
                        }
                    }
                }, 500);
                return;
            }

            notifyError(message);
            setState(State.IDLE);
        }
    };

    private void connectToDevice(BluetoothDevice device) {
        try {
            setState(State.CONNECTING);
            reconnectAttempts = 0;
            userDisconnected = false;
            bluetoothGatt = device.connectGatt(context, false, gattCallback);
        } catch (SecurityException e) {
            notifyError("蓝牙权限不足");
            setState(State.IDLE);
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (status != BluetoothGatt.GATT_SUCCESS && state == State.CONNECTING) {
                    try {
                        gatt.close();
                    } catch (Exception ignored) {
                    }
                    bluetoothGatt = null;
                    startBleScan();
                } else if (userDisconnected) {
                    userDisconnected = false;
                    setState(State.DISCONNECTED);
                    commandSender = null;
                } else {
                    setState(State.DISCONNECTED);
                    commandSender = null;
                    attemptReconnect();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(BleCommandSender.SERVICE_UUID);
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(BleCommandSender.CHARACTERISTIC_UUID);
                    if (characteristic != null) {
                        enableNotification(gatt, characteristic);
                        commandSender = new BleCommandSender(gatt);
                        setState(State.CONNECTED);
                        reconnectAttempts = 0;
                    } else {
                        notifyError("未找到数据特征");
                    }
                } else {
                    notifyError("未找到 GATT 服务");
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onDataReceived(data);
                }
            });
        }
    };

    private void enableNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        gatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD_UUID);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
        }
    }

    private void attemptReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            notifyError("重连失败，请手动连接");
            setState(State.IDLE);
            return;
        }

        reconnectAttempts++;
        Log.d(TAG, "尝试重连 " + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS);

        mainHandler.postDelayed(() -> {
            if (bluetoothGatt != null) {
                bluetoothGatt.connect();
            }
        }, RECONNECT_DELAY);
    }

    private void setState(State newState) {
        state = newState;
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onStateChanged(newState);
            }
        });
    }

    private void notifyError(String message) {
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onError(message);
            }
        });
    }

    public void close() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        commandSender = null;
        setState(State.IDLE);
    }
}
