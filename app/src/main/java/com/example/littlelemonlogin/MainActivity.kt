package com.example.littlelemonlogin

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothHeadset
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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.littlelemonlogin.ui.theme.LittleLemonLoginTheme


private var bluetoothAdapter: BluetoothAdapter? = null
private val MY_MAC_ADDRESS = "FC:91:5D:64:FE:5F"
private val TAG = "WENDEE TEST"

// Stops scanning after 10 seconds.
private val SCAN_PERIOD: Long = 10000
private var scanning = false
private var isGattConnected = false

class MainActivity : ComponentActivity() {

//    private var bluetoothAdapter: BluetoothAdapter? = null
//    private val MY_MAC_ADDRESS = "FC:91:5D:64:FE:5F"
//    private val TAG = "WENDEE TEST"
//
//    // Stops scanning after 10 seconds.
//    private val SCAN_PERIOD: Long = 10000
//    private var scanning = false
//    private var isGattConnected = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LittleLemonLoginTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    LoginScreen()
                }
            }
        }
        requestBTPermission()
        checkAndEnableBluetooth()
    }

    private fun isPermissionGranted(permissionToCheck: String): Boolean {
        return ActivityCompat.checkSelfPermission(this@MainActivity, permissionToCheck) ==
                PackageManager.PERMISSION_GRANTED
    }

    /* Request all Bluetooth permissions when the activity starts. */
    private fun requestBTPermission() {
        val bluetoothManager = ActivityCompat.getSystemService(this, BluetoothManager::class.java) ?: return
        bluetoothAdapter = bluetoothManager.adapter ?: return

        if (!isPermissionGranted(Manifest.permission.BLUETOOTH_SCAN) ||
            !isPermissionGranted(Manifest.permission.BLUETOOTH_ADVERTISE) ||
            !isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
            !isPermissionGranted(Manifest.permission.BLUETOOTH_CONNECT)) {
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

    private val requestBluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissions ->
                permissions.forEach { p ->
                    if (p.value == false) {
                        Log.d(TAG, "$p is not permitted")
                    }
                }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkAndEnableBluetooth() {
        if (bluetoothAdapter?.isEnabled == false) {
            startBluetoothIntentForResult.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } else {
            // start scanning
            Log.d(TAG, "checkAndEnableBluetooth")
            scanDevice()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val startBluetoothIntentForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->

                if (result.resultCode != Activity.RESULT_OK) {
                    // Uncomment the following line to force turn on
                    // checkAndEnableBluetooth()

                } else {
                    // start scanning
                    Log.d(TAG, "startBluetoothIntentForResult")
                    scanDevice()

            }
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
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            Log.d(TAG, "address -> $deviceAddress")
            Log.d(TAG, "bondState -> ${device.bondState}, previous -> $previousState")
        }

    }

    @SuppressLint("MissingPermission")
    private fun connectBondedDevice(device: BluetoothDevice) {
        Log.d(TAG, "connectBondedDevice")
        bluetoothAdapter?.getProfileProxy(this@MainActivity, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                Log.d(TAG, "onServiceConnected(profile=$profile, proxy=$proxy)")
                val connectMethod = BluetoothHeadset::class.java.getDeclaredMethod(
                    "connect", BluetoothDevice::class.java
                ).apply { isAccessible = true }

                connectMethod.invoke(proxy, device)
            }

            override fun onServiceDisconnected(profile: Int) {
                Log.d(TAG, "onServiceDisconnected(profile=$profile)")
            }
        }, BluetoothProfile.HEADSET)
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
        this.registerReceiver(broadcastReceiver, filter)

        val devices = bluetoothAdapter?.bondedDevices
        if (devices != null && devices.size > 0) {
            Log.d(TAG, "devices -> $devices")

            // TODO: something is wrong here
            connectBondedDevice(devices.first())
//            connectGattServer(devices.first())
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
                        this@MainActivity,
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
    private fun connectGattServer(device: BluetoothDevice): BluetoothGatt ? {
         val bluetoothGattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
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
                    // gatt?.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // disconnected from the GATT Server
                    gatt?.close()
                    Log.d(TAG, "GATT STATE_DISCONNECTED $status new -> $newState")
                }
                Log.d(TAG, "gatt callback $status new -> $newState bondStatus -> ${device.bondState}")
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "No permission when trying to connect the gatt server.")
            return null
        }
        Log.d(TAG, "device $device ready to connect")
        return device.connectGatt(this@MainActivity, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE)
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
                this@MainActivity,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothLeScanner?.stopScan(localCallback)
        } else {
            Log.e(TAG, "No permission when trying to stop scanning.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "destroyed!!")
        unregisterReceiver(broadcastReceiver)
    }

}

@Composable
fun LoginScreen(){
    val context = LocalContext.current
    var username by rememberSaveable {
        mutableStateOf("")
    }
    var password by rememberSaveable {
        mutableStateOf("")
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(
                id = R.drawable.littlelemonlogo),
            contentDescription = "Logo Image",
            modifier = Modifier.padding(10.dp)
        )
        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(text = "Username") },
            modifier = Modifier.padding(10.dp)
        )
        TextField(
            value = password,
            visualTransformation = PasswordVisualTransformation(),
            onValueChange = { password = it },
            label = { Text(text = "Password") },
            modifier = Modifier.padding(10.dp)
        )
        Button(
            onClick = {
                      // ScanDevice()
            },
            colors = ButtonDefaults.buttonColors(
                Color(0xFF495E57),
            ),
            modifier = Modifier.padding(10.dp)
        ) {
            Text(
                text = "Login",
                color = Color(0xFFEDEFEE)
            )
        }
    }
}
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview(){
    LoginScreen()
}