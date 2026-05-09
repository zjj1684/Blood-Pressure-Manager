package com.bloodpressure.app.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.bloodpressure.app.ble.BleConnectionManager;
import com.bloodpressure.app.ble.BleService;

public class SharedBleViewModel extends ViewModel {

    private BleService bleService;

    private final MutableLiveData<BleConnectionManager.State> connectionState =
            new MutableLiveData<>(BleConnectionManager.State.IDLE);
    private final MutableLiveData<BleService> bleServiceLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> systolic = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> diastolic = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> heartRate = new MutableLiveData<>(0);

    public void setBleService(BleService service) {
        this.bleService = service;
        if (service != null) {
            connectionState.setValue(service.getConnectionState());
            service.setConnectionStateCallback(state -> connectionState.postValue(state));
        }
        bleServiceLiveData.postValue(service);
    }

    public BleService getBleService() {
        return bleService;
    }

    public LiveData<BleService> getBleServiceLiveData() {
        return bleServiceLiveData;
    }

    public LiveData<BleConnectionManager.State> getConnectionState() {
        return connectionState;
    }

    public LiveData<Integer> getSystolic() {
        return systolic;
    }

    public LiveData<Integer> getDiastolic() {
        return diastolic;
    }

    public LiveData<Integer> getHeartRate() {
        return heartRate;
    }

    public void updateBpValues(int sys, int dia, int hr) {
        systolic.setValue(sys);
        diastolic.setValue(dia);
        heartRate.setValue(hr);
    }

    public boolean isConnected() {
        return bleService != null &&
                bleService.getConnectionState() == BleConnectionManager.State.CONNECTED;
    }

    public boolean isCollecting() {
        return bleService != null && bleService.isCollecting();
    }
}
