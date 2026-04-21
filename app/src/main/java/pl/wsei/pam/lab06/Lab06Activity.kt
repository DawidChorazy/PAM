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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import pl.wsei.pam.lab01.Lab01Activity
import pl.wsei.pam.lab02.Lab02Activity
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
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(pl.wsei.pam.lab01.R.drawable.ic_launcher_foreground)
            .setContentTitle(intent?.getStringExtra(titleExtra) ?: "Zadanie")
            .setContentText(intent?.getStringExtra(messageExtra) ?: "Termin!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(notificationID, notification)
    }
}

class NotificationHandler(private val context: Context) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    fun showSimpleNotification() {
        val notification = NotificationCompat.Builder(context, channelID)
            .setContentTitle("Lab Center")
            .setContentText("Zarządzaj swoimi projektami.")
            .setSmallIcon(pl.wsei.pam.lab01.R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationID + 1, notification)
    }
}

interface AppContainer { val notificationHandler: NotificationHandler }
class AppDataContainer(private val context: Context) : AppContainer {
    override val notificationHandler: NotificationHandler by lazy { NotificationHandler(context) }
}

enum class TaskFilter(val label: String) { Wszystkie("Wszystkie"), Aktywne("Do zrobienia"), Wykonane("Zaliczone") }

class Lab06Activity : ComponentActivity() {
    companion object { var container: AppContainer? = null }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelID, "Laboratoria", NotificationManager.IMPORTANCE_HIGH)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        container = AppDataContainer(this)
        setContent { MaterialTheme { Surface(modifier = Modifier.fillMaxSize()) { MainScreen() } } }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val tasks = remember { mutableStateListOf<TodoTask>().apply { addAll(initialTodoTasks()) } }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(Unit) { if (!permissionState.status.isGranted) permissionState.launchPermissionRequest() }
    }

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            ListScreen(navController, tasks) { task ->
                val index = tasks.indexOfFirst { it.title == task.title }
                if (index != -1) tasks[index] = tasks[index].copy(isDone = !tasks[index].isDone)
            }
        }
        composable("form") { FormScreen(navController) { tasks.add(it); navController.popBackStack() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(navController: NavController, tasks: List<TodoTask>, onTaskToggle: (TodoTask) -> Unit) {
    var currentFilter by rememberSaveable { mutableStateOf(TaskFilter.Wszystkie) }
    val context = LocalContext.current

    val filteredTasks = when (currentFilter) {
        TaskFilter.Wszystkie -> tasks
        TaskFilter.Aktywne -> tasks.filter { !it.isDone && it.activityClass == null }
        TaskFilter.Wykonane -> tasks.filter { it.isDone }
    }

    Scaffold(
        floatingActionButton = { FloatingActionButton(onClick = { navController.navigate("form") }, shape = CircleShape) { Icon(Icons.Default.Add, null) } },
        topBar = { 
            CenterAlignedTopAppBar(
                title = { Text("Centrum Labów", fontWeight = FontWeight.Black) },
                actions = { IconButton(onClick = { Lab06Activity.container?.notificationHandler?.showSimpleNotification() }) { Icon(Icons.Default.Notifications, null) } }
            ) 
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = currentFilter.ordinal) {
                TaskFilter.entries.forEach { filter ->
                    Tab(selected = currentFilter == filter, onClick = { currentFilter = filter }, text = { Text(filter.label) })
                }
            }
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredTasks, key = { it.title }) { item ->
                    ListItem(
                        item = item, 
                        onCheckedChange = { onTaskToggle(item) },
                        onRunClick = {
                            item.activityClass?.let { cls ->
                                val intent = Intent(context, cls)
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ListItem(item: TodoTask, onCheckedChange: (Boolean) -> Unit, onRunClick: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (item.isDone) Color.Gray else Color.Unspecified)
                Text(if (item.activityClass != null) "Laboratorium" else "Termin: ${item.deadline.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}", fontSize = 12.sp, color = Color.Gray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.activityClass != null) {
                    Button(onClick = onRunClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Uruchom", fontSize = 12.sp)
                    }
                } else {
                    Checkbox(checked = item.isDone, onCheckedChange = onCheckedChange)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(navController: NavController, onAddTask: (TodoTask) -> Unit) {
    var title by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(onDismissRequest = { showDatePicker = false }, 
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { deadline = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }; showDatePicker = false }) { Text("OK") } }
        ) { DatePicker(state) }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Dodaj zadanie") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tytuł") }, modifier = Modifier.fillMaxWidth())
            OutlinedCard(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp)) { Icon(Icons.Default.DateRange, null); Spacer(Modifier.width(12.dp)); Text(deadline.toString()) }
            }
            Button(onClick = { if (title.isNotBlank()) onAddTask(TodoTask(title, deadline, false, Priority.Medium)) }, modifier = Modifier.fillMaxWidth()) { Text("Zapisz") }
        }
    }
}

enum class Priority { High, Medium, Low }
data class TodoTask(val title: String, val deadline: LocalDate, val isDone: Boolean, val priority: Priority, val activityClass: Class<*>? = null)

fun initialTodoTasks() = listOf(
    TodoTask("Lab 01", LocalDate.now(), false, Priority.Low, Lab01Activity::class.java),
    TodoTask("Lab 02", LocalDate.now(), false, Priority.Medium, Lab02Activity::class.java),
    TodoTask("Programming", LocalDate.of(2024, 4, 18), false, Priority.Low),
    TodoTask("Teaching", LocalDate.of(2024, 5, 12), false, Priority.High),
    TodoTask("Learning", LocalDate.of(2024, 6, 28), true, Priority.Low)
)
