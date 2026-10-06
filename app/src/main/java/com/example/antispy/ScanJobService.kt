package com.example.antispy

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class ScanJobService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        Thread {
            try { runCheck(applicationContext) } finally { jobFinished(params, false) }
        }.start()
        return true // работа идёт в фоне
    }

    override fun onStopJob(params: JobParameters?) = false

    companion object {
        private const val JOB_ID = 1001
        private const val CHANNEL = "antispy_alerts"
        private const val NOTIF_THRESHOLD = 40 // уведомлять только при заметном риске

        /** Запускает проверку каждые 15 минут (минимум для Android), переживает перезагрузку. */
        fun schedule(ctx: Context) {
            val js = ctx.getSystemService(JobScheduler::class.java)
            if (js.getPendingJob(JOB_ID) != null) return
            val job = JobInfo.Builder(JOB_ID, ComponentName(ctx, ScanJobService::class.java))
                .setPeriodic(15 * 60 * 1000L)
                .setPersisted(true)
                .build()
            js.schedule(job)
        }

        private fun runCheck(ctx: Context) {
            val prefs = ctx.getSharedPreferences("antispy", Context.MODE_PRIVATE)
            val reported = prefs.getStringSet("reported", emptySet())!!.toSet()

            val found = Scanner.scan(ctx, NOTIF_THRESHOLD)
            // ключ = пакет + риск: если риск вырос (например, включили доступность), тревога повторится
            val keys = found.associateBy { "${it.pkg}:${it.score}" }
            val fresh = keys.filterKeys { it !in reported }.values

            prefs.edit().putStringSet("reported", keys.keys).apply()
            if (fresh.isEmpty()) return
            notify(ctx, fresh.toList())
        }

        private fun notify(ctx: Context, items: List<Finding>) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return

            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Предупреждения о слежке", NotificationManager.IMPORTANCE_HIGH)
            )

            val pi = PendingIntent.getActivity(
                ctx, 0, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val title = if (items.size == 1) "⚠ Подозрительное приложение: ${items[0].label}"
            else "⚠ Подозрительных приложений: ${items.size}"
            val text = items.joinToString("\n") { "${it.label}: ${it.reasons.first()}" }

            val n = Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(items.first().reasons.first())
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            nm.notify(2001, n)
        }
    }
}
