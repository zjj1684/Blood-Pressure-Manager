package com.bloodpressure.app.ble;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.bloodpressure.app.BloodPressureApplication;
import com.bloodpressure.app.R;
import com.bloodpressure.app.data.model.SampleData;
import com.bloodpressure.app.ui.main.MainActivity;

import java.util.List;

public class BleService extends Service {

    private static final String TAG = "BleService";
    private static final int NOTIFICATION_ID = 1;

    public interface DataCallback {
        void onSamplesReceived(List<SampleData> samples);
        void onConnectionStateChanged(BleConnectionManager.State state);
        void onError(String message);
    }

    public interface ConnectionStateCallback {
        void onConnectionStateChanged(BleConnectionManager.State state);
    }

    private final IBinder binder = new LocalBinder();
    private BleConnectionManager connectionManager;
    private BleDataParser dataParser;
    private Handler mainHandler;
    private DataCallback dataCallback;
    private ConnectionStateCallback connectionStateCallback;
    private boolean isCollecting = false;
    private long currentSessionId = -1;
    private int totalSamples = 0;

    public class LocalBinder extends Binder {
        public BleService getService() {
            return BleService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        connectionManager = new BleConnectionManager(this);
        dataParser = new BleDataParser();

        connectionManager.setCallback(new BleConnectionManager.Callback() {
            @Override
            public void onStateChanged(BleConnectionManager.State state) {
                mainHandler.post(() -> {
                    if (connectionStateCallback != null) {
                        connectionStateCallback.onConnectionStateChanged(state);
                    }
                    if (dataCallback != null) {
                        dataCallback.onConnectionStateChanged(state);
                    }
                });
            }

            @Override
            public void onDataReceived(byte[] data) {
                BleDataParser.ParseResult result = dataParser.parse(data);
                if (result != null && isCollecting) {
                    totalSamples += result.samples.size();
                    mainHandler.post(() -> {
                        if (dataCallback != null) {
                            dataCallback.onSamplesReceived(result.samples);
                        }
                    });
                }
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    if (dataCallback != null) {
                        dataCallback.onError(message);
                    }
                });
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setDataCallback(DataCallback callback) {
        this.dataCallback = callback;
    }

    public void setConnectionStateCallback(ConnectionStateCallback callback) {
        this.connectionStateCallback = callback;
    }

    public void startScan() {
        connectionManager.startScan();
    }

    public void disconnect() {
        stopCollecting();
        connectionManager.disconnect();
    }

    public BleConnectionManager.State getConnectionState() {
        return connectionManager.getState();
    }

    public boolean isBluetoothEnabled() {
        return connectionManager.isBluetoothEnabled();
    }

    public boolean startCollecting(long sessionId) {
        if (connectionManager.getState() != BleConnectionManager.State.CONNECTED) {
            if (dataCallback != null) {
                dataCallback.onError("设备未连接");
            }
            return false;
        }

        BleCommandSender sender = connectionManager.getCommandSender();
        if (sender == null) {
            if (dataCallback != null) {
                dataCallback.onError("指令发送器为空");
            }
            return false;
        }

        boolean sent = sender.sendStartCommand();
        if (sent) {
            isCollecting = true;
            currentSessionId = sessionId;
            totalSamples = 0;
            dataParser.reset();
            Log.d(TAG, "开始采集，会话ID: " + sessionId);
        } else {
            if (dataCallback != null) {
                dataCallback.onError("writeCharacteristic 返回 false");
            }
        }
        return sent;
    }

    public void stopCollecting() {
        if (isCollecting) {
            BleCommandSender sender = connectionManager.getCommandSender();
            if (sender != null) {
                sender.sendStopCommand();
            }
            isCollecting = false;
            Log.d(TAG, "停止采集，总采样数: " + totalSamples);
        }
    }

    public long getCurrentSessionId() {
        return currentSessionId;
    }

    public int getTotalSamples() {
        return totalSamples;
    }

    public boolean isCollecting() {
        return isCollecting;
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, BloodPressureApplication.CHANNEL_ID_BLE_SERVICE)
                .setContentTitle("血压监测")
                .setContentText("BLE 服务运行中")
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setContentIntent(pendingIntent)
                .build();
    }

    @Override
    public void onDestroy() {
        stopCollecting();
        if (connectionManager != null) {
            connectionManager.close();
        }
        super.onDestroy();
    }
}
