package com.bloodpressure.app.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.UUID;

public class BleCommandSender {

    public static final UUID SERVICE_UUID = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB");
    public static final UUID CHARACTERISTIC_UUID = UUID.fromString("0000FFF1-0000-1000-8000-00805F9B34FB");

    private static final byte CMD_START = 0x01;
    private static final byte CMD_STOP = 0x02;

    private final BluetoothGatt gatt;

    public BleCommandSender(BluetoothGatt gatt) {
        this.gatt = gatt;
    }

    public boolean sendStartCommand() {
        return sendCommand(CMD_START);
    }

    public boolean sendStopCommand() {
        return sendCommand(CMD_STOP);
    }

    private boolean sendCommand(byte command) {
        if (gatt == null) {
            Log.e("BleCommandSender", "sendCommand failed: gatt is null");
            return false;
        }

        BluetoothGattService service = gatt.getService(SERVICE_UUID);
        if (service == null) {
            Log.e("BleCommandSender", "sendCommand failed: service not found, UUID=" + SERVICE_UUID);
            // List all available services
            for (BluetoothGattService s : gatt.getServices()) {
                Log.d("BleCommandSender", "Available service: " + s.getUuid());
            }
            return false;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
        if (characteristic == null) {
            Log.e("BleCommandSender", "sendCommand failed: characteristic not found, UUID=" + CHARACTERISTIC_UUID);
            for (BluetoothGattCharacteristic c : service.getCharacteristics()) {
                Log.d("BleCommandSender", "Available characteristic: " + c.getUuid()
                        + " props=" + c.getProperties());
            }
            return false;
        }

        Log.d("BleCommandSender", "Sending command: 0x" + String.format("%02X", command));
        characteristic.setValue(new byte[]{command});
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        boolean result = gatt.writeCharacteristic(characteristic);
        Log.d("BleCommandSender", "writeCharacteristic result: " + result);
        return result;
    }
}
