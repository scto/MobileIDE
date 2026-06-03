package rrzt.web.web_bridge

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("MissingPermission")
class BluetoothManager(
    private val context: Context,
    private val sendResult: (String, Boolean, String) -> Unit
) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val gattMap = ConcurrentHashMap<String, BluetoothGatt>()
    private var scanCallbackId: String? = null
    
    // Key: "$address-$charUUID-read/write" -> Value: callbackId
    private val operationCallbacks = ConcurrentHashMap<String, String>()
    
    // Key: "$address-$charUUID-notify" -> Value: callbackId
    private val notifyCallbacks = ConcurrentHashMap<String, String>()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            scanCallbackId?.let { cid ->
                result?.device?.let { device ->
                    val json = JSONObject()
                    json.put("name", device.name ?: "Unknown")
                    json.put("address", device.address)
                    json.put("rssi", result.rssi)
                    sendResult(cid, true, json.toString())
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            scanCallbackId?.let { cid ->
                sendResult(cid, false, "Scan failed: $errorCode")
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val address = gatt.device.address
            val cid = operationCallbacks.remove("$address-connect")
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                    cid?.let { sendResult(it, true, "{\"status\":\"connected\",\"address\":\"$address\"}") }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    gatt.close()
                    gattMap.remove(address)
                    cid?.let { sendResult(it, true, "{\"status\":\"disconnected\",\"address\":\"$address\"}") }
                }
            } else {
                gatt.close()
                gattMap.remove(address)
                cid?.let { sendResult(it, false, "Connection failed status: $status") }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
             // connection callback handled in onConnectionStateChange usually covers the "ready" state, 
             // but strictly speaking, we should wait for services.
             // For simplicity, we rely on connection callback.
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            val address = gatt.device.address
            val uuid = characteristic.uuid.toString()
            val cid = operationCallbacks.remove("$address-$uuid-read")
            
            if (cid != null) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val data = Base64.encodeToString(characteristic.value, Base64.NO_WRAP)
                    sendResult(cid, true, data)
                } else {
                    sendResult(cid, false, "Read failed: $status")
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            val address = gatt.device.address
            val uuid = characteristic.uuid.toString()
            val cid = operationCallbacks.remove("$address-$uuid-write")
            
            if (cid != null) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    sendResult(cid, true, "Write success")
                } else {
                    sendResult(cid, false, "Write failed: $status")
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val address = gatt.device.address
            val uuid = characteristic.uuid.toString()
            val cid = notifyCallbacks["$address-$uuid-notify"]
            
            if (cid != null) {
                val data = Base64.encodeToString(characteristic.value, Base64.NO_WRAP)
                val json = JSONObject()
                json.put("uuid", uuid)
                json.put("data", data)
                sendResult(cid, true, json.toString())
            }
        }
    }

    fun isAvailable(): Boolean = bluetoothAdapter != null

    fun isEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    fun enable(): Boolean {
        return bluetoothAdapter?.enable() ?: false
    }

    fun startScan(callbackId: String) {
        if (bluetoothAdapter?.isEnabled != true) {
            sendResult(callbackId, false, "Bluetooth disabled")
            return
        }
        scanCallbackId = callbackId
        bluetoothAdapter.bluetoothLeScanner?.startScan(scanCallback)
    }

    fun stopScan() {
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        scanCallbackId = null
    }

    fun connect(address: String, callbackId: String) {
        val device = bluetoothAdapter?.getRemoteDevice(address)
        if (device == null) {
            sendResult(callbackId, false, "Device not found")
            return
        }
        operationCallbacks["$address-connect"] = callbackId
        val gatt = device.connectGatt(context, false, gattCallback)
        gattMap[address] = gatt
    }

    fun disconnect(address: String, callbackId: String) {
        val gatt = gattMap[address]
        if (gatt != null) {
            operationCallbacks["$address-connect"] = callbackId 
            gatt.disconnect()
        } else {
            sendResult(callbackId, true, "Already disconnected")
        }
    }
    
    fun getServices(address: String, callbackId: String) {
        val gatt = gattMap[address]
        if (gatt == null) {
            sendResult(callbackId, false, "Not connected")
            return
        }
        
        val services = JSONArray()
        gatt.services.forEach { service ->
            val serviceJson = JSONObject()
            serviceJson.put("uuid", service.uuid.toString())
            val chars = JSONArray()
            service.characteristics.forEach { chara ->
                 val charJson = JSONObject()
                 charJson.put("uuid", chara.uuid.toString())
                 charJson.put("properties", chara.properties)
                 chars.put(charJson)
            }
            serviceJson.put("characteristics", chars)
            services.put(serviceJson)
        }
        sendResult(callbackId, true, services.toString())
    }

    fun read(address: String, serviceUuid: String, charUuid: String, callbackId: String) {
        val gatt = gattMap[address]
        val service = gatt?.getService(UUID.fromString(serviceUuid))
        val characteristic = service?.getCharacteristic(UUID.fromString(charUuid))
        
        if (gatt != null && characteristic != null) {
            operationCallbacks["$address-$charUuid-read"] = callbackId
            gatt.readCharacteristic(characteristic)
        } else {
            sendResult(callbackId, false, "Characteristic not found")
        }
    }

    fun write(address: String, serviceUuid: String, charUuid: String, dataBase64: String, callbackId: String) {
        val gatt = gattMap[address]
        val service = gatt?.getService(UUID.fromString(serviceUuid))
        val characteristic = service?.getCharacteristic(UUID.fromString(charUuid))
        
        if (gatt != null && characteristic != null) {
            try {
                characteristic.value = Base64.decode(dataBase64, Base64.NO_WRAP)
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT 
                operationCallbacks["$address-$charUuid-write"] = callbackId
                gatt.writeCharacteristic(characteristic)
            } catch(e: Exception) {
                sendResult(callbackId, false, "Write error: ${e.message}")
            }
        } else {
            sendResult(callbackId, false, "Characteristic not found")
        }
    }
    
    fun notify(address: String, serviceUuid: String, charUuid: String, enable: Boolean, callbackId: String) {
         val gatt = gattMap[address]
        val service = gatt?.getService(UUID.fromString(serviceUuid))
        val characteristic = service?.getCharacteristic(UUID.fromString(charUuid))
        
        if (gatt != null && characteristic != null) {
            if (enable) {
                notifyCallbacks["$address-$charUuid-notify"] = callbackId
            } else {
                notifyCallbacks.remove("$address-$charUuid-notify")
            }
            gatt.setCharacteristicNotification(characteristic, enable)
            
            val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            if (descriptor != null) {
                descriptor.value = if (enable) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
            
            sendResult(callbackId, true, "Notify set to $enable")
        } else {
            sendResult(callbackId, false, "Characteristic not found")
        }
    }
}
