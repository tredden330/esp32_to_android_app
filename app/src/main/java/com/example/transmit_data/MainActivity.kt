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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
    val context = LocalContext.current

    // Compose states
    val message = remember { mutableStateOf("waiting...") }
    val success = remember { mutableStateOf(false) }
    val numMessages = remember { mutableIntStateOf(0) }
    val dataLoad = remember { mutableStateOf("hi") }
    val dataHistory = remember { mutableStateListOf<Int>() }
    val webSocketState = remember { mutableStateOf<WebSocket?>(null) }

    val uiScope = rememberCoroutineScope() // for safe main-thread updates

    val ledOn = remember { mutableStateOf(false) }

    Surface(color = MaterialTheme.colorScheme.primary) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 50.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Only connect if not already connected
            if (!success.value) {
                Text(text = "waiting for connection...")

                // Create WebSocket once
                val okHttpClient = OkHttpClient()
                val request = Request.Builder().url("ws://192.168.4.1/ws").build()

                // Open WebSocket
                val webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        uiScope.launch {
                            message.value = "Connected to ESP32"
                            success.value = true
                            webSocketState.value = webSocket // store reference for button
                        }

                        // Optional: log to Room database
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val max = MainActivity.main_database.LogDao().getRunIDs().maxOrNull()
                                val id = if (max == null) 1 else max + 1
                                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm ss.SSS")
                                val current = LocalDateTime.now().format(formatter)
                                val newLog = RunLog(runID = id, time = current.toString())
                                MainActivity.main_database.LogDao().newLog(newLog)
                                Log.e("rooom", MainActivity.main_database.LogDao().getLogs().toString())
                            } catch (e: Exception) {
                                Log.e("rooom", "Error creating log: ${e.message}")
                            }
                        }

                        webSocket.send("identify")
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        uiScope.launch {
                            numMessages.value += 1

                            if (text.contains("Instrument")) {
                                message.value = text
                            } else {
                                dataLoad.value = text
                                dataHistory.add(text.toInt())
                                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm ss.SSS")
                                val current = LocalDateTime.now().format(formatter)
                                addData(current)
                            }
                        }
                    }

                    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                        uiScope.launch {
                            message.value = "Received binary message!"
                        }
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        uiScope.launch {
                            message.value = "Connection failed: ${t.message}"
                        }
                    }
                })
            } else {
                // Already connected → show UI
                Text(text = message.value)
                Text(text = dataLoad.value, modifier = Modifier.offset(y = 20.dp))

                // Graph
                Graph(modifier, dataHistory)

                FilledTonalButton(
                    onClick = {
                        val command = if (ledOn.value) "off" else "on"
                        webSocketState.value?.send(command)
                        ledOn.value = !ledOn.value
                        message.value = "Sent $command command!"
                    },
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    Text(text = if (ledOn.value) "Turn LED OFF" else "Turn LED ON")
                }
            }
        }
    }
}