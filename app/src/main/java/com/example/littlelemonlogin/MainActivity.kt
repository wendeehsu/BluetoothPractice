package com.example.littlelemonlogin

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
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
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.littlelemonlogin.ui.theme.LittleLemonLoginTheme
import android.Manifest


class MainActivity : ComponentActivity() {

    private val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val BLUETOOTH_REQUEST_CODE = 1
    private val TAG = "WENDEE TEST"

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000
    private var scanning = false

    @RequiresApi(Build.VERSION_CODES.M)
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
        // Scan
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

    private fun checkAndEnableBluetooth() {
        if (bluetoothAdapter?.isEnabled == false) {
            startBluetoothIntentForResult.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } else {
            // start scanning
        }
    }

    private val startBluetoothIntentForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
                if (result.resultCode != Activity.RESULT_OK) {
                    // Uncomment the following line to force turn on
                    // checkAndEnableBluetooth()
                }
            Log.d(TAG, "result -> ${result.resultCode}")
        }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("MissingPermission")
    private fun scanLeDevice() {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        if (!scanning) { // Stops scanning after a pre-defined scan period.
            Handler(Looper.getMainLooper()).postDelayed({
                scanning = false
                bluetoothLeScanner?.stopScan(leScanCallback)
            }, SCAN_PERIOD)
            scanning = true
            bluetoothLeScanner?.startScan(leScanCallback)

//            bluetoothLeScanner.startScan(leScanCallback, ScanSettings.Builder().setLegacy(false).build())
        } else {
            scanning = false
            bluetoothLeScanner?.stopScan(leScanCallback)
        }
    }

//    private val leDeviceListAdapter = LeDeviceListAdapter()
    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d(TAG, "result: $result / device: ${result.device}")
//            leDeviceListAdapter.addDevice(result.device)
//            leDeviceListAdapter.notifyDataSetChanged()
        }
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
                      if (username == "Darian" && password == "littlelemon") {
                          Toast.makeText(context, "Welcome", Toast.LENGTH_SHORT).show()
                      } else {
                          Toast.makeText(context, "Ooooops", Toast.LENGTH_SHORT).show()
                      }
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