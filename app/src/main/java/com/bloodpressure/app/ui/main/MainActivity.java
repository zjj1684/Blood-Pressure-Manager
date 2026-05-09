package com.bloodpressure.app.ui.main;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bloodpressure.app.R;
import com.bloodpressure.app.ble.BleService;
import com.bloodpressure.app.ui.history.HistoryFragment;
import com.bloodpressure.app.ui.monitor.MonitorFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_BLE_PERMISSIONS = 1001;

    private BleService bleService;
    private boolean serviceBound = false;
    private SharedBleViewModel sharedBleViewModel;
    private boolean serviceStarted = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BleService.LocalBinder binder = (BleService.LocalBinder) service;
            bleService = binder.getService();
            serviceBound = true;
            sharedBleViewModel.setBleService(bleService);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            sharedBleViewModel.setBleService(null);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedBleViewModel = new ViewModelProvider(this).get(SharedBleViewModel.class);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();
            if (id == R.id.nav_monitor) {
                fragment = new MonitorFragment();
            } else if (id == R.id.nav_history) {
                fragment = new HistoryFragment();
            }
            if (fragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit();
            }
            return true;
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_monitor);
        }

        // Check permissions and start service
        if (hasBlePermissions()) {
            startAndBindBleService();
        } else {
            requestBlePermissions();
        }
    }

    private boolean hasBlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, "android.permission.BLUETOOTH_SCAN") == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, "android.permission.BLUETOOTH_CONNECT") == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestBlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, new String[]{
                    "android.permission.BLUETOOTH_SCAN",
                    "android.permission.BLUETOOTH_CONNECT",
                    "android.permission.ACCESS_FINE_LOCATION"
            }, REQUEST_BLE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLE_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                startAndBindBleService();
            } else {
                Toast.makeText(this, "需要蓝牙权限才能连接设备", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startAndBindBleService() {
        if (serviceStarted) return;
        serviceStarted = true;

        Intent intent = new Intent(this, BleService.class);
        try {
            startForegroundService(intent);
        } catch (Exception e) {
            try {
                startService(intent);
            } catch (Exception ignored) {
            }
        }
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
        super.onDestroy();
    }
}
