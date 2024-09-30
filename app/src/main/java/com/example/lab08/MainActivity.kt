package com.example.lab08

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.work.OneTimeWorkRequestBuilder
import kotlinx.coroutines.launch
import com.example.lab08.ui.theme.Lab08Theme
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {
    private lateinit var handler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab08Theme {
                val db = Room.databaseBuilder(
                    applicationContext,
                    TaskDatabase::class.java,
                    "task_db"
                ).build()


                val taskDao = db.taskDao()
                val viewModel = TaskViewModel(taskDao)


                TaskScreen(viewModel)
            }
            scheduleTaskReminder()
        }
        handler = Handler(Looper.getMainLooper())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

    }
    override fun onPause() {
        super.onPause()
        scheduleTaskReminder()
    }
    private fun scheduleTaskReminder() {
        // Programar la notificación para 1 minuto después
        handler.postDelayed({
            val taskReminderRequest = OneTimeWorkRequestBuilder<TaskReminderWorker>()
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(this).enqueue(taskReminderRequest)
        }, 0) // Sin demora adicional
    }
}
@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var newTaskDescription by remember { mutableStateOf("") }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var editedDescription by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (editingTask == null) {
            // Campo para agregar una nueva tarea
            TextField(
                value = newTaskDescription,
                onValueChange = { newTaskDescription = it },
                label = { Text("Nueva tarea") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (newTaskDescription.isNotEmpty()) {
                        viewModel.addTask(newTaskDescription)
                        newTaskDescription = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("Agregar tarea")
            }
        } else {
            // Campo para editar una tarea existente
            TextField(
                value = editedDescription,
                onValueChange = { editedDescription = it },
                label = { Text("Editar tarea") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    editingTask?.let {
                        viewModel.updateTask(it.copy(description = editedDescription))
                        editingTask = null
                        editedDescription = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("Guardar cambios")
            }

            Button(
                onClick = {
                    editingTask = null
                    editedDescription = ""
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("Cancelar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        tasks.forEach { task ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = task.description)

                Row {
                    IconButton(onClick = { viewModel.toggleTaskCompletion(task) }) {
                        Icon(
                            imageVector = if (task.isCompleted) Icons.Default.Close else Icons.Default.Check,
                            contentDescription = if (task.isCompleted) "Marcar como pendiente" else "Marcar como completada"
                        )
                    }

                    IconButton(onClick = {
                        editingTask = task
                        editedDescription = task.description
                    }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar tarea")
                    }
                }
            }
        }

        Button(
            onClick = { coroutineScope.launch { viewModel.deleteAllTasks() } },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Eliminar todas las tareas")
        }
    }
}