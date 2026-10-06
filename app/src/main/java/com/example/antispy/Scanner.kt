package com.example.antispy

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

data class Finding(
    val pkg: String,
    val label: String,
    var score: Int = 0,
    val reasons: MutableList<String> = mutableListOf()
)

object Scanner {

    private val dangerous = mapOf(
        "android.permission.RECORD_AUDIO" to "микрофон",
        "android.permission.CAMERA" to "камера",
        "android.permission.ACCESS_FINE_LOCATION" to "точная геолокация",
        "android.permission.ACCESS_BACKGROUND_LOCATION" to "геолокация в фоне",
        "android.permission.READ_SMS" to "чтение SMS",
        "android.permission.RECEIVE_SMS" to "перехват SMS",
        "android.permission.READ_CALL_LOG" to "журнал звонков",
        "android.permission.READ_CONTACTS" to "контакты",
        "android.permission.READ_EXTERNAL_STORAGE" to "файлы"
    )

    private fun enabledPackages(ctx: Context, setting: String): Set<String> {
        val raw = Settings.Secure.getString(ctx.contentResolver, setting) ?: return emptySet()
        return raw.split(":")
            .mapNotNull { it.substringBefore("/").takeIf { s -> s.isNotBlank() } }
            .toSet()
    }

    private fun installer(pm: PackageManager, pkg: String): String? = try {
        if (Build.VERSION.SDK_INT >= 30) pm.getInstallSourceInfo(pkg).installingPackageName
        else @Suppress("DEPRECATION") pm.getInstallerPackageName(pkg)
    } catch (e: Exception) { null }

    fun scan(ctx: Context, minScore: Int = 25): List<Finding> {
        val pm = ctx.packageManager
        val accessibility = enabledPackages(ctx, "enabled_accessibility_services")
        val notifListeners = enabledPackages(ctx, "enabled_notification_listeners")
        val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admins = dpm.activeAdmins?.map { it.packageName }?.toSet() ?: emptySet()

        val result = mutableListOf<Finding>()

        @Suppress("DEPRECATION")
        for (app in pm.getInstalledApplications(0)) {
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            if (isSystem || app.packageName == ctx.packageName) continue

            val f = Finding(app.packageName, pm.getApplicationLabel(app).toString())

            if (app.packageName in accessibility) {
                f.score += 40; f.reasons += "включена служба доступности (читает экран и ввод)"
            }
            if (app.packageName in admins) {
                f.score += 30; f.reasons += "администратор устройства (трудно удалить)"
            }
            if (app.packageName in notifListeners) {
                f.score += 25; f.reasons += "читает все уведомления (коды, мессенджеры)"
            }

            val inst = installer(pm, app.packageName)
            if (inst != "com.android.vending") {
                f.score += 10; f.reasons += "установлено не из Google Play (${inst ?: "неизвестно"})"
            }

            if (pm.getLaunchIntentForPackage(app.packageName) == null) {
                f.score += 15; f.reasons += "нет значка запуска (скрытое приложение)"
            }

            try {
                @Suppress("DEPRECATION")
                val info = pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
                val granted = mutableListOf<String>()
                info.requestedPermissions?.forEachIndexed { i, p ->
                    val ok = ((info.requestedPermissionsFlags?.get(i) ?: 0) and
                            PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                    if (ok && dangerous.containsKey(p)) granted += dangerous[p]!!
                }
                if (granted.size >= 3) {
                    f.score += 5 * granted.size
                    f.reasons += "выданы права: ${granted.joinToString()}"
                }
            } catch (_: Exception) {}

            if (f.score >= minScore) result += f
        }
        return result.sortedByDescending { it.score }
    }
}
