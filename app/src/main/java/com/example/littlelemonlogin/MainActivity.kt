package com.example.littlelemonlogin

import android.app.Activity.RESULT_OK
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.example.littlelemonlogin.ui.theme.LittleLemonLoginTheme
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat

class MainActivity : ComponentActivity() {

    private val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val TAG = "WENDEE TEST"

    @RequiresApi(Build.VERSION_CODES.M)
    private var bluetoothEnableResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
                    bluetoothAdapter = bluetoothManager.getAdapter()
                }
                Activity.RESULT_CANCELED -> {
                    // oh no :(
                }
            }
        }

    @RequiresApi(Build.VERSION_CODES.M)
    private val requestBluetoothPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                bluetoothEnableResultLauncher.launch(enableBluetoothIntent)
            } else {
                // some sort of retry
            }
        }


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

        // bluetooth set up
        setupViews()
        checkBluetoothEnabled()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupViews() {
        Log.d(TAG, "setupViews")
        // rerun the permissions check logic if it was already denied
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBluetoothPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            requestBluetoothPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkBluetoothEnabled() {
        Log.d(TAG, "checkBluetoothEnabled")
        // bluetooth set up
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter = bluetoothManager.getAdapter()

        // displays message if the device doesn't support bluetooth
        if (bluetoothAdapter == null) {
            Toast.makeText(
                this,
                "Your device does not support bluetooth. This may lead to limited functionality of this application",
                Toast.LENGTH_SHORT
            ).show()
        }

        // check to see if user grants permission to bluetooth
        val registerForResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data

            }
            Log.d(TAG, result.toString())

        }


        // activate bluetooth permission
        if (bluetoothAdapter?.isEnabled == false) {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                /**
                 * We DON'T have Bluetooth permissions. We have to get them before we can ask the
                 *  user to enable Bluetooth
                 */

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (shouldShowRequestPermissionRationale(android.Manifest.permission.BLUETOOTH_CONNECT)) {
                        //put toast about why need bluetooth
                    }

                } else {
                    requestBluetoothPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
                }
            } else {
                if (shouldShowRequestPermissionRationale(android.Manifest.permission.BLUETOOTH)) {
                    //put toast about why need bluetooth
                } else {
                    requestBluetoothPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH)
                }
            }

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