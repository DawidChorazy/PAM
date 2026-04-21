package pl.wsei.pam.lab06

import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import pl.wsei.pam.lab01.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

const val notificationID = 121
const val channelID = "Lab06 channel"
const val titleExtra = "title"
const val messageExtra = "message"

class NotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("Lab06", "Odebrano alarm!")
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(intent?.getStringExtra(titleExtra) ?: "Zadanie")
            .setContentText(intent?.getStringExtra(messageExtra) ?: "Zbliża się termin!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()
        manager.notify(notificationID, notification)
    }
}

class NotificationHandler(private val context: Context) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    fun showSimpleNotification() {
        val notification = NotificationCompat.Builder(context, channelID)
            .setContentTitle("Test: Przycisk działa")
            .setContentText("To powiadomienie wywołałeś ręcznie ikoną Settings.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationID + 1, notification)
    }
}

interface AppContainer {
    val notificationHandler: NotificationHandler
}

class AppDataContainer(private val context: Context) : AppContainer {
    override val notificationHandler: NotificationHandler by lazy { NotificationHandler(context) }
}

class Lab06Activity : ComponentActivity() {
    companion object {
        var container: AppContainer? = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Powiadomienia Lab06"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelID, name, importance).apply {
                description = "Kanał dla zadań"
                enableVibration(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleRepeatingAlarm(time: Long, title: String, message: String) {
        val intent = Intent(applicationContext, NotificationBroadcastReceiver::class.java).apply {
            putExtra(titleExtra, title)
            putExtra(messageExtra, message)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(applicationContext, notificationID, intent, flags)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.cancel(pendingIntent)
        
        // Ustawienie powtarzania co 4 godziny
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, time, 4 * 3600 * 1000L, pendingIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        container = AppDataContainer(this)

        // ALARM TESTOWY: Pojawi się za 10 sekund od teraz
        scheduleRepeatingAlarm(System.currentTimeMillis() + 10000L, "Alarm Testowy", "Powiadomienie pojawiło się po 10 sek.")
        Toast.makeText(this, "Alarm testowy ustawiony na za 10s", Toast.LENGTH_LONG).show()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val tasks = remember { mutableStateListOf<TodoTask>().apply { addAll(initialTodoTasks()) } }
    val context = androidx.compose.ui.platform.LocalContext.current

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(Unit) {
            if (!permissionState.status.isGranted) {
                permissionState.launchPermissionRequest()
            }
        }
    }

    fun updateNearestTaskAlarm(currentTasks: List<TodoTask>) {
        val nearestTask = currentTasks.filter { !it.isDone }.minByOrNull { it.deadline }
        nearestTask?.let { task ->
            val deadlineMillis = task.deadline.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val alarmStartTime = deadlineMillis - (24 * 3600 * 1000L) // 24h przed

            val finalStartTime = if (alarmStartTime < System.currentTimeMillis()) {
                System.currentTimeMillis() + 2000L // Jeśli termin już blisko, ustaw za 2 sekundy
            } else {
                alarmStartTime
            }

            (context as? Lab06Activity)?.scheduleRepeatingAlarm(
                finalStartTime,
                "Nadchodzące zadanie: ${task.title}",
                "Termin: ${task.deadline.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}"
            )
        }
    }

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            ListScreen(navController, tasks) { index ->
                tasks[index] = tasks[index].copy(isDone = !tasks[index].isDone)
                updateNearestTaskAlarm(tasks)
            }
        }
        composable("form") {
            FormScreen(navController) { newTask ->
                tasks.add(newTask)
                updateNearestTaskAlarm(tasks)
                navController.popBackStack()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(navController: NavController, title: String, showBackIcon: Boolean, route: String, onSaveClick: (() -> Unit)? = null) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        title = { Text(title) },
        navigationIcon = { if (showBackIcon) IconButton(onClick = { navController.navigate(route) }) { Icon(Icons.Default.ArrowBack, "Back") } },
        actions = {
            if (onSaveClick != null) {
                OutlinedButton(onClick = onSaveClick, modifier = Modifier.padding(end = 8.dp)) { Text("Zapisz") }
            } else {
                IconButton(onClick = { Lab06Activity.container?.notificationHandler?.showSimpleNotification() }) {
                    Icon(Icons.Default.Settings, "Settings")
                }
                IconButton(onClick = { /* Home */ }) { Icon(Icons.Default.Home, "Home") }
            }
        }
    )
}

@Composable
fun ListScreen(navController: NavController, tasks: List<TodoTask>, onTaskToggle: (Int) -> Unit) {
    Scaffold(
        floatingActionButton = { FloatingActionButton(shape = CircleShape, onClick = { navController.navigate("form") }) { Icon(Icons.Default.Add, "Add") } },
        topBar = { AppTopBar(navController, "Zadania", false, "form") }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            itemsIndexed(tasks) { index, item ->
                ListItem(item, onCheckedChange = { onTaskToggle(index) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(navController: NavController, onAddTask: (TodoTask) -> Unit) {
    var title by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf(LocalDate.now()) }
    var priority by remember { mutableStateOf(Priority.Medium) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = deadline.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dateState.selectedDateMillis?.let { deadline = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Anuluj") } }
        ) { DatePicker(dateState) }
    }

    Scaffold(topBar = { AppTopBar(navController, "Dodaj zadanie", true, "list", onSaveClick = { if (title.isNotBlank()) onAddTask(TodoTask(title, deadline, false, priority)) }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tytuł") }, modifier = Modifier.fillMaxWidth())
            Text("Priorytet", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { p -> FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.name) }) }
            }
            Text("Termin", fontWeight = FontWeight.Bold)
            OutlinedCard(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, null); Spacer(Modifier.width(12.dp)); Text(deadline.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                }
            }
        }
    }
}

@Composable
fun ListItem(item: TodoTask, onCheckedChange: (Boolean) -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Termin: ${item.deadline.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}", fontSize = 14.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                val color = when(item.priority) { Priority.High -> Color.Red; Priority.Medium -> Color(0xFFFFA500); Priority.Low -> Color(0xFF2E7D32) }
                Text(item.priority.name, color = color, fontWeight = FontWeight.Bold)
                Checkbox(checked = item.isDone, onCheckedChange = onCheckedChange)
            }
        }
    }
}

enum class Priority { High, Medium, Low }
data class TodoTask(val title: String, val deadline: LocalDate, val isDone: Boolean, val priority: Priority)
fun initialTodoTasks() = listOf(TodoTask("Praca domowa", LocalDate.now().plusDays(2), false, Priority.High))
