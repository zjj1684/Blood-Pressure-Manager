package com.bloodpressure.app.ui.monitor;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bloodpressure.app.R;
import com.bloodpressure.app.ble.BleConnectionManager;
import com.bloodpressure.app.ble.BleService;
import com.bloodpressure.app.ui.main.SharedBleViewModel;
import com.bloodpressure.app.waveform.WaveformView;

public class MonitorFragment extends Fragment {

    private static final int REQUEST_BLE_PERMISSIONS = 1001;

    private SharedBleViewModel sharedViewModel;
    private WaveformView waveformView;
    private TextView tvStatus;
    private TextView tvSystolic;
    private TextView tvDiastolic;
    private TextView tvHeartRate;
    private Button btnConnect;
    private Button btnStartStop;

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

        btnConnect.setOnClickListener(v -> onConnectClicked());
        btnStartStop.setOnClickListener(v -> onStartStopClicked());

        sharedViewModel.getConnectionState().observe(getViewLifecycleOwner(), state -> {
            tvStatus.setText(getStateText(state));
            boolean connected = state == BleConnectionManager.State.CONNECTED;
            btnStartStop.setEnabled(connected);
            btnConnect.setText(connected ? R.string.btn_disconnect : R.string.btn_connect);
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
            }
        });
    }

    private void setupBleCallbacks(BleService service) {
        service.setDataCallback(new BleService.DataCallback() {
            @Override
            public void onSamplesReceived(java.util.List<com.bloodpressure.app.data.model.SampleData> samples) {
                waveformView.addBatch(samples);
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
            btnStartStop.setText(R.string.btn_start);
            Toast.makeText(requireContext(), "已发送停止指令 (0x02)", Toast.LENGTH_SHORT).show();
        } else {
            long sessionId = System.currentTimeMillis();
            boolean ok = service.startCollecting(sessionId);
            if (ok) {
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
