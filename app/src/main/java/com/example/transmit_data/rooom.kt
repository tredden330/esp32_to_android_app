package com.example.transmit_data

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random


@Composable
fun InspectorScreen(modifier: Modifier = Modifier) {

    //var showOnboarding = remember { mutableStateOf("hi there") }
    var numDataPoints = remember { mutableIntStateOf(0) }

    Surface(color = MaterialTheme.colorScheme.primary) {
        Column(modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            FilledTonalButton(onClick = { addData("test") }, modifier = Modifier.padding(vertical = 1.dp)) {
                Text(text = "Add random data")
            }
            FilledTonalButton(onClick = { listData(numDataPoints) }, modifier = Modifier.padding(vertical = 1.dp)) {
                Text(text = "List data")
            }
            FilledTonalButton(onClick = { deleteData() }, modifier = Modifier.padding(vertical = 1.dp)) {
                Text(text = "deleteData")
            }
            FilledTonalButton(onClick = { createLog() }, modifier = Modifier.padding(vertical = 1.dp)) {
                Text(text = "createLog")
            }
            FilledTonalButton(onClick = { listLogs(numDataPoints) }, modifier = Modifier.padding(vertical = 1.dp)) {
                Text(text = "List Logs")
            }
            FilledTonalButton(onClick = { deleteLogs() }, modifier = Modifier.padding(vertical = 1.dp)) {
                Text(text = "deleteLogs")
            }
            Text(numDataPoints.intValue.toString())
        }
    }
}

fun listLogs(numDataPoints: MutableIntState) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val dataPoints = MainActivity.main_database.LogDao().getLogs()
            numDataPoints.intValue = dataPoints.size
        } catch (e: Exception) {
            Log.e("rooom", "Error creating user: ${e.message}")
        }
    }
}

fun deleteData() {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val dataPoints = MainActivity.main_database.dataDao().deleteData()
            Log.e("rooom", dataPoints.toString())
        } catch (e: Exception) {
            Log.e("rooom", "Error creating user: ${e.message}")
        }
    }
}

fun deleteLogs() {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val dataPoints = MainActivity.main_database.LogDao().deleteLogs()
            Log.e("rooom", dataPoints.toString())
        } catch (e: Exception) {
            Log.e("rooom", "Error creating user: ${e.message}")
        }
    }
}

fun createLog() {
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
}

fun listData(numDataPoints: MutableIntState) {

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val dataPoints = MainActivity.main_database.dataDao().getAllData()
            Log.e("rooom", MainActivity.main_database.dataDao().getTimes().toString())
            Log.e("rooom", dataPoints.size.toString())
            numDataPoints.intValue = dataPoints.size
        } catch (e: Exception) {
            Log.e("rooom", "Error creating user: ${e.message}")
        }
    }
}

fun addData(time: String) {
    val newData = DataPoint(runID = 1, data = Random.nextInt(1,100), time = time)
    CoroutineScope(Dispatchers.IO).launch {
        try {
            MainActivity.main_database.dataDao().newData(newData)
            //Log.d("rooom", "data added")
        } catch (e: Exception) {
            Log.e("rooom", "Error creating user: ${e.message}")
        }
    }
}

@Entity(tableName = "data")
data class DataPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runID: Int,
    val data: Int,
    val time: String
)

@Entity(tableName = "logs")
data class RunLog(
    @PrimaryKey(autoGenerate = true) val runID: Int,
    val time: String
)

@Dao
interface DataDao {
    @Query("SELECT * FROM data")
    suspend fun getAllData(): List<DataPoint>

    @Query("SELECT time FROM data")
    suspend fun getTimes(): List<String>

    @Insert
    suspend fun newData(user: DataPoint)

    @Query("DELETE FROM data")
    suspend fun deleteData()
}

@Dao
interface LogDao {
    @Query("SELECT * FROM Logs")
    suspend fun getLogs(): List<RunLog>

    @Query("SELECT runID FROM logs")
    suspend fun getRunIDs(): List<Int>

    @Insert
    suspend fun newLog(log: com.example.transmit_data.RunLog)

    @Query("DELETE FROM Logs")
    suspend fun deleteLogs()
}



@Database(entities = [DataPoint::class, RunLog::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dataDao(): DataDao

    abstract fun LogDao(): LogDao
}

