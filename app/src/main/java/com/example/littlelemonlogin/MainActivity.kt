package com.example.littlelemonlogin

import android.os.Build
import android.os.Bundle
import android.util.Log
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.littlelemonlogin.ui.theme.LittleLemonLoginTheme

class MainActivity : ComponentActivity() {
    val mBluetooth = Bluetooth()

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
                    LoginScreen(mBluetooth)
                }
            }
        }
        mBluetooth.requestBTPermission(this)
//        mBluetooth.checkAndEnableBluetooth()
    }


    override fun onDestroy() {
        super.onDestroy()
    }

    fun test () {
        Log.d("?", "???")
    }

}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LoginScreen(btService: Bluetooth){
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
        Button(
            onClick = {
                  btService.checkAndEnableBluetooth()
            },
            colors = ButtonDefaults.buttonColors(
                Color(0xFF495E57),
            ),
            modifier = Modifier.padding(10.dp)
        ) {
            Text(
                text = "Connect",
                color = Color(0xFFEDEFEE)
            )
        }
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
                text = "Disconnect",
                color = Color(0xFFEDEFEE)
            )
        }
    }
}
@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview(){
    LoginScreen(btService = Bluetooth())
}