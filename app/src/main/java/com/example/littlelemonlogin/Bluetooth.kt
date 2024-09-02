package com.example.littlelemonlogin

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

class Bluetooth(private val context: Context) {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val MY_MAC_ADDRESS = "FC:91:5D:64:FE:5F"
    private val TAG = "WENDEE TEST"
    private var pairedDevice : BluetoothDevice? = null

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 30000
    private var scanning = false
    private var isGattConnected = false

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//    }
//
//    override fun onDestroy() {
//        unregisterReceiver(broadcastReceiver)
//        super.onDestroy()
//    }

    private fun isPermissionGranted(permissionToCheck: String): Boolean {
        Log.d(TAG, "checking permission -> $permissionToCheck | Granted -> ${ActivityCompat.checkSelfPermission(context, permissionToCheck) == PackageManager.PERMISSION_GRANTED}")
        return ActivityCompat.checkSelfPermission(context, permissionToCheck) ==
                PackageManager.PERMISSION_GRANTED
    }

    /* Request all Bluetooth permissions when the activity starts. */
    fun requestBTPermission(requestBluetoothPermissionLauncher: ActivityResultLauncher<Array<String>>) {
        Log.d(TAG, "in requestBTPermission")
        val bluetoothManager = ActivityCompat.getSystemService(context, BluetoothManager::class.java) ?: return
        bluetoothAdapter = bluetoothManager.adapter ?: return

        if (!isPermissionGranted(Manifest.permission.BLUETOOTH_SCAN) ||
            !isPermissionGranted(Manifest.permission.BLUETOOTH_ADVERTISE) ||
            !isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
            !isPermissionGranted(Manifest.permission.BLUETOOTH_CONNECT)) {

            Log.d(TAG, "oooooooooooops!")
            requestBluetoothPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            Log.d(TAG, "all granted")
        }
    }



    @RequiresApi(Build.VERSION_CODES.O)
    fun checkAndEnableBluetooth(startBluetoothIntentForResult : ActivityResultLauncher<Intent>) {
        if (bluetoothAdapter?.isEnabled == false) {
            startBluetoothIntentForResult.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } else {
            // start scanning
            Log.d(TAG, "checkAndEnableBluetooth")
        }
        scanDevice()
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        // onReceive called at ACTION_BOND_STATE_CHANGED
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            Log.d(TAG, "intent -> $intent ,action -> $action")
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
            val previousState = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE)
            val deviceAddress = device.address
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            Log.d(TAG, "address -> $deviceAddress")
            Log.d(TAG, "bondState -> ${device.bondState}, previous -> $previousState")
        }

    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("MissingPermission")
    private fun connectBondedDevice(device: BluetoothDevice) {
        Log.d(TAG, "connectBondedDevice")
        if (device.bondState == BluetoothDevice.BOND_BONDED) {
            connectGattServer(device)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    private fun scanDevice() {
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        val filters = mutableListOf(ScanFilter.Builder().build())
        val scanSettings = ScanSettings.Builder()
            .setLegacy(false)
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        // Register for broadcasts when a device is discovered.
        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        context.registerReceiver(broadcastReceiver, filter)

        val devices = bluetoothAdapter?.bondedDevices
        if (devices != null && devices.size > 0) {
            Log.d(TAG, "devices -> $devices")

            // TODO: something is wrong here
            connectBondedDevice(devices.first())

            return
        }

        if (!scanning) {
            // Stops scanning after a pre-defined scan period.
            Handler(Looper.getMainLooper()).postDelayed({
                scanning = false
                bluetoothLeScanner?.stopScan(leScanCallback)
            }, SCAN_PERIOD)
            scanning = true
            Log.d(TAG, "start scanning")
            bluetoothLeScanner?.startScan(leScanCallback)
        } else {
            scanning = false
            bluetoothLeScanner?.stopScan(leScanCallback)
        }
    }

    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if (isGattConnected) return
            Log.d(TAG, "device -> ${result.device}")
            if (result.device.toString() == MY_MAC_ADDRESS) {
                connectGattServer(result.device) ?: return
                isGattConnected = true
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.e(TAG, "No permission to create bond.")
                    return
                }
                result.device.createBond()
                stopAllScanning()
            }
        }
    }

    /* Connect to a GATT server on [device] */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun connectGattServer(device: BluetoothDevice): BluetoothGatt? {
        val bluetoothGattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e(TAG, "GATT failed.")
                    gatt?.close()
                }

                // Handle cases for BluetoothGatt.GATT_SUCCESS
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    // successfully connected to the GATT Server
                    Log.d(TAG, "GATT STATE_CONNECTED $status new -> $newState bondStatus -> ${device.bondState}")
                    pairedDevice = device
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // disconnected from the GATT Server
                    gatt?.close()
                    Log.d(TAG, "GATT STATE_DISCONNECTED $status new -> $newState")
                    pairedDevice = null
                }
                Log.d(TAG, "gatt callback $status new -> $newState bondStatus -> ${device.bondState}")
            }
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "No permission when trying to connect the gatt server.")
            return null
        }
        Log.d(TAG, "device $device ready to connect")
        return device.connectGatt(context, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private fun stopAllScanning() {
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        val localCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                Log.d(TAG, "Stop scan from callback.")
            }
        }
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothLeScanner?.stopScan(localCallback)
        } else {
            Log.e(TAG, "No permission when trying to stop scanning.")
        }
    }
}