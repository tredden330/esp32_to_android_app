package com.example.transmit_data

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DataScreen() {
    val coroutineScope = rememberCoroutineScope()
    //var showOnboarding = remember { mutableStateOf("hi there") }
    var logs = remember { mutableStateListOf(RunLog(
        runID = 0,
        time = 0.toString()
    )) }

    fetchLogs(coroutineScope, logs)

    Surface(color = MaterialTheme.colorScheme.primary) {
        Column(modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header item
                item {
                    Text(text = "Logs:")
                }

                // Display logs dynamically
                items(logs) { log ->
                    Text(
                        text = log.time,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 16.dp)
                    )
                }

                // Footer item
                item {
                    Text(
                        text = "Total Logs: ${logs.size}",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            Text(text = logs.size.toString(), modifier = Modifier.padding(vertical = 10.dp))
            logs.forEach() { log ->
                Text(text = log.time, modifier = Modifier.padding(vertical = 10.dp))
            }
        }
    }
}

fun fetchLogs(coroutineScope: kotlinx.coroutines.CoroutineScope, logs: MutableList<RunLog>) {
    coroutineScope.launch {
        val databaseLogs = MainActivity.main_database.LogDao().getLogs()
        logs.clear()
        logs.addAll(databaseLogs)
        Log.d("datamenu", logs.size.toString())
    }
}