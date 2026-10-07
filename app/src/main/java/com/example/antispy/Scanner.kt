package com.example.antispy

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

data class Finding(
    val label: String,
    val pkg: String,
    val score: Int,
    val reasons: List<String>
)

object Scanner {
    fun scan(context: Context): List<Finding> {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val findings = mutableListOf<Finding>()

        for (app in installedApps) {
            if (app.flags and ApplicationInfo.FLAG_SYSTEM != 0) continue

            var score = 0
            val reasons = mutableListOf<String>()

            try {
                val pkgInfo = pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
                val requestedPermissions = pkgInfo.requestedPermissions ?: arrayOf()

                for (perm in requestedPermissions) {
                    when (perm) {
                        android.Manifest.permission.READ_SMS,
                        android.Manifest.permission.RECEIVE_SMS -> {
                            score += 30
                            reasons.add("Чтение/приём SMS")
                        }
                        android.Manifest.permission.RECORD_AUDIO -> {
                            score += 25
                            reasons.add("Запись аудио (микрофон)")
                        }
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION -> {
                            score += 20
                            reasons.add("Доступ к геолокации")
                        }
                        android.Manifest.permission.READ_CONTACTS -> {
                            score += 15
                            reasons.add("Чтение контактов")
                        }
                    }
                }
            } catch (_: Exception) {
                // Игнорируем ошибки доступа к отдельным пакетам
            }

            if (score > 0) {
                val label = pm.getApplicationLabel(app).toString()
                findings.add(Finding(label, app.packageName, score, reasons))
            }
        }

        return findings.sortedByDescending { it.score }
    }
}