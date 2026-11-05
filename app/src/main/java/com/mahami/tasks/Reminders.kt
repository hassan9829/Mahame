package com.mahami.tasks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object Reminders {
    const val CHANNEL = "tasks"

    fun schedule(ctx: Context, t: Task) {
        val wm = WorkManager.getInstance(ctx)
        val name = "task_${t.id}"
        val d = t.dueAt
        val now = System.currentTimeMillis()
        if (t.done || !t.remind || d == null || d <= now) {
            wm.cancelUniqueWork(name)
            return
        }
        val req = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(d - now, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("id" to t.id))
            .build()
        wm.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, req)
    }

    fun cancel(ctx: Context, id: Long) {
        WorkManager.getInstance(ctx).cancelUniqueWork("task_$id")
    }
}

class ReminderWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        val id = inputData.getLong("id", -1L)
        val t = AppDatabase.get(applicationContext).dao().get(id) ?: return Result.success()
        if (t.done) return Result.success()

        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(Reminders.CHANNEL, "تذكيرات المهام", NotificationManager.IMPORTANCE_HIGH)
        )
        val pi = PendingIntent.getActivity(
            applicationContext, 0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(applicationContext, Reminders.CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("حان وقت المهمة")
            .setContentText(t.title)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(t.id.toInt(), n)
        return Result.success()
    }
}
