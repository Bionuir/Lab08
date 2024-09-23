package com.example.lab08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.room.Room
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking

class TaskReminderWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val tasks = getPendingTasks()

        if (tasks.isNotEmpty()) {
            createNotificationChannel()
            val taskList = tasks.joinToString("\n") { it.description }
            val notification = NotificationCompat.Builder(applicationContext, "task_reminder_channel")
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Tareas pendientes")
                .setContentText("Recuerda completar tus tareas:\n$taskList")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle(NotificationCompat.BigTextStyle().bigText("Recuerda completar tus tareas:\n$taskList"))
                .build()

            NotificationManagerCompat.from(applicationContext).notify(1, notification)
        }

        return Result.success()
    }

    private fun getPendingTasks(): List<Task> {
        val db = Room.databaseBuilder(
            applicationContext,
            TaskDatabase::class.java,
            "task_db"
        ).build()

        return runBlocking {
            db.taskDao().getAllTasks().filter { !it.isCompleted } // Filtrar tareas pendientes
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Task Reminder Channel"
            val descriptionText = "Channel for task reminders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("task_reminder_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
