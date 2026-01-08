package com.example.transmit_data

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.transmit_data.ui.theme.Transmit_dataTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    companion object {
        lateinit var main_database: AppDatabase
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        main_database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "main_database"
        ).fallbackToDestructiveMigration()
            .build()
        enableEdgeToEdge()
        setContent {
            Transmit_dataTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }


}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    var screenState = remember { mutableIntStateOf(0) }
    when (screenState.intValue) {
        0 -> {
            OnboardingScreen(onConnectClicked = { screenState.intValue = 1 }, onInspectorClicked = { screenState.intValue = 2 }, onHistoricalClicked = {screenState.intValue = 3})
        }
        1 -> {
            NetworkScreen()
        }
        2 -> {
            InspectorScreen()
        }
        3 -> {
            DataScreen()
        }
    }
}

@Composable
fun OnboardingScreen(modifier: Modifier = Modifier, onConnectClicked: () -> Unit, onInspectorClicked: () -> Unit, onHistoricalClicked: () -> Unit) {

    //var showOnboarding = remember { mutableStateOf("hi there") }

    Surface(color = MaterialTheme.colorScheme.primary) {
        Column(modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Welcome to a remote instrument app!!", modifier = Modifier.padding(vertical = 10.dp))
            Text(text = "by Thomas", modifier = Modifier.padding(vertical = 10.dp))
            FilledTonalButton(onClick = onConnectClicked, modifier = Modifier.padding(vertical = 1.dp)) {
                Text(text = "Connect to Instrument")
            }
            FilledTonalButton(onClick = onInspectorClicked, modifier = Modifier.padding(vertical = 10.dp)) {
                Text(text = "Database Inspector")
            }
            FilledTonalButton(onClick = onHistoricalClicked, modifier = Modifier.padding(vertical = 10.dp)) {
                Text(text = "View Data")
            }
        }
    }
}

@Composable
fun NetworkScreen(modifier: Modifier = Modifier) {

    Surface(color = MaterialTheme.colorScheme.primary) {

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 50.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally) {

            var message = remember { mutableStateOf("") }
            var success = remember { mutableStateOf(false) }
            var numMessages = remember { mutableIntStateOf(0) }
            var dataLoad = remember { mutableStateOf("hi") }
            var dataHistory = remember { mutableListOf<Int>() }



            if (!success.value) {
                Text(text = "waiting...")

                val okHttpClient = OkHttpClient()   //wss://echo.websocket.org is a great website to test the websocket client against a server
                val request = Request.Builder().url("ws://192.168.4.1/ws").build()  //connect to esp32
                //val request = Request.Builder().url("wss://echo.websocket.org").build()
                val webSocket = okHttpClient.newWebSocket(request, object: WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {

                        message.value = response.headers.toString()
                        success.value = true

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val max = MainActivity.main_database.LogDao().getRunIDs().maxOrNull()
                                var id = 0
                                if (max == null) {
                                    id = 1
                                } else {
                                    id = max+1
                                }
                                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm ss.SSS")
                                val current = LocalDateTime.now().format(formatter)
                                val newLog = RunLog(runID = id, time = current.toString())
                                MainActivity.main_database.LogDao().newLog(newLog)
                                Log.e("rooom", MainActivity.main_database.LogDao().getLogs().toString())
                            } catch (e: Exception) {
                                Log.e("rooom", "Error creating user: ${e.message}")
                            }
                        }

                        val result = webSocket.send("identify")
                        //Log.d("12345", "message status: $result")
                    }

                    //if string data is passed through websocket this is called
                    override fun onMessage(webSocket: WebSocket, text: String) {
                        numMessages.value += 1
                        if (text.contains("Instrument")) { //first message the server sends will contain "Instrument"
                            message.value = text
                        } else {
                            dataLoad.value = text
                            dataHistory.add(text.toInt())
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm ss.SSS")
                            val current = LocalDateTime.now().format(formatter)
                            addData(current)
                            //Log.d("12345", "incoming message: $current")
                        }
                    }

                    //if message is in byte form this is called
                    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                        super.onMessage(webSocket, bytes)
                        message.value = "message received!!"

                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: Response?
                    ) {
                        super.onFailure(webSocket, t, response)
                        //Log.d("12345", t.message.toString())
                        message.value = t.message.toString()
                    }
                })
            }    else {
                Text(text = message.value)
                Text(text = dataLoad.value, modifier.offset(y=20.dp))
                Graph(modifier, dataHistory)
            }
        }
    }
}