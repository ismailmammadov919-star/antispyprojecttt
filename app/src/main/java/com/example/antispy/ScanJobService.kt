package com.example.antispy

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context

class ScanJobService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        Thread {
            Scanner.scan(applicationContext)
            jobFinished(params, false)
        }.start()
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true
    }

    companion object {
        private const val JOB_ID = 1001

        fun schedule(context: Context) {
            val componentName = ComponentName(context, ScanJobService::class.java)
            val builder = JobInfo.Builder(JOB_ID, componentName)
                .setPeriodic(24 * 60 * 60 * 1000L)
                .setPersisted(true)

            val scheduler = context.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
            scheduler.schedule(builder.build())
        }
    }
}