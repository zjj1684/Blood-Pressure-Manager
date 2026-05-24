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
