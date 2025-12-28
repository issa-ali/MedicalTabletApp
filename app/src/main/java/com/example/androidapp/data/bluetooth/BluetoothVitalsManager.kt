package com.example.androidapp.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

data class VitalsData(
    val heartRate: Int = 0,
    val spo2: Int = 0,
    val temperature: Float = 0f,
    val systolic: Int = 0,
    val diastolic: Int = 0,
    val ecg: Int = 0
)

class BluetoothVitalsManager(private val context: Context) {
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected
    
    private val _vitals = MutableStateFlow(VitalsData())
    val vitals: StateFlow<VitalsData> = _vitals
    
    // Debug: Expose last raw string to UI
    private val _lastMessage = MutableStateFlow("")
    val lastMessage: StateFlow<String> = _lastMessage
    
    private var socket: BluetoothSocket? = null
    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SPP UUID
    
    private val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    // Find paired device named "MedicalTablet_Sensor"
    @SuppressLint("MissingPermission")
    suspend fun connectToDevice(): Boolean = withContext(Dispatchers.IO) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) return@withContext false
        
        val device = adapter.bondedDevices.find { it.name == "MedicalTablet_Sensor" }
        if (device == null) {
            Log.e("Bluetooth", "Device 'MedicalTablet_Sensor' not found among paired devices")
            return@withContext false
        }
        
        try {
            socket = device.createRfcommSocketToServiceRecord(uuid)
            socket?.connect()
            _isConnected.value = true
            
            // Start listening loop in background, don't block this function
            scope.launch { listenForData() }
            
            return@withContext true
        } catch (e: Exception) {
            Log.e("Bluetooth", "Connection failed", e)
            close()
            return@withContext false
        }
    }
    
    private suspend fun listenForData() {
        try {
            val reader = BufferedReader(InputStreamReader(socket!!.inputStream))
            while (true) {
                val line = reader.readLine() ?: break
                _lastMessage.value = line
                parseData(line)
            }
        } catch (e: Exception) {
            Log.e("Bluetooth", "Read error", e)
        } finally {
            close()
        }
    }
    
    fun sendCommand(command: String) {
        try {
            if (socket?.isConnected == true) {
                socket?.outputStream?.write(command.toByteArray())
                Log.d("Bluetooth", "Sent command: $command")
            }
        } catch (e: Exception) {
            Log.e("Bluetooth", "Error sending command", e)
        }
    }

    private fun parseData(json: String) {
        try {
            // Sanitize: "nan" is invalid JSON, replace with 0
            val cleanJson = json.replace("nan", "0", ignoreCase = true)
            
            // Expected: {"mode": "H", "hr": 75, "spo2": 98, "temp": 36.6, "ecg": 1024}
            val obj = JSONObject(cleanJson)
            val currentData = _vitals.value
            
            // Update only fields present in JSON to avoid overwriting with zeros
            val newData = currentData.copy(
                heartRate = if (obj.has("hr") && obj.getInt("hr") > 0) obj.getInt("hr") else currentData.heartRate,
                spo2 = if (obj.has("spo2") && obj.getInt("spo2") > 0) obj.getInt("spo2") else currentData.spo2,
                temperature = if (obj.has("temp")) obj.getDouble("temp").toFloat() else currentData.temperature,
                systolic = if (obj.has("sys")) obj.getInt("sys") else currentData.systolic,
                diastolic = if (obj.has("dia")) obj.getInt("dia") else currentData.diastolic,
                ecg = if (obj.has("ecg")) obj.getInt("ecg") else currentData.ecg
            )
            _vitals.value = newData
        } catch (e: Exception) {
            // Log.w("Bluetooth", "Parse error: $json")
        }
    }
    
    fun close() {
        try {
            socket?.close()
        } catch (e: Exception) {}
        socket = null
        _isConnected.value = false
    }
}
