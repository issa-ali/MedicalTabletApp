package com.example.androidapp.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.data.bluetooth.BluetoothVitalsManager
import com.example.androidapp.data.bluetooth.VitalsData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VitalsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val bluetoothManager = BluetoothVitalsManager(application)
    
    val isConnected: StateFlow<Boolean> = bluetoothManager.isConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        
    val vitals: StateFlow<VitalsData> = bluetoothManager.vitals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VitalsData())
        
    val lastMessage: StateFlow<String> = bluetoothManager.lastMessage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
        
    suspend fun connect(): Boolean {
        return bluetoothManager.connectToDevice()
    }
    
    fun startHeartRate() {
        bluetoothManager.sendCommand("H")
    }
    
    fun startTemp() {
        bluetoothManager.sendCommand("T")
    }
    
    fun startBP() {
        bluetoothManager.sendCommand("B")
    }
    
    fun startECG() {
        bluetoothManager.sendCommand("E")
    }
    
    fun stopSensors() {
        bluetoothManager.sendCommand("S")
    }
    
    override fun onCleared() {
        super.onCleared()
        bluetoothManager.close()
    }
}
