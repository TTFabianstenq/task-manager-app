package com.taskmanager.app.process

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Process
import android.provider.Settings

class ProcessRepository(private val context: Context) {

    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun usageAccessIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun deviceMemory(): DeviceMemory {
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)
        val total = info.totalMem / (1024 * 1024)
        val avail = info.availMem / (1024 * 1024)
        val used = (total - avail).coerceAtLeast(0)
        val percent = if (total > 0) ((used * 100) / total).toInt() else 0
        return DeviceMemory(total, avail, used, percent)
    }

    fun listApps(): List<AppProcess> {
        val pm = context.packageManager
        val now = System.currentTimeMillis()
        val runningByPkg = runningProcessesByPackage()
        val lastUsed = lastUsedByPackage(now)

        val launchable = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            0
        ).map { it.activityInfo.packageName }.toSet()

        val packages = (launchable + runningByPkg.keys + lastUsed.keys).toSet()

        return packages.mapNotNull { pkg ->
            if (pkg == context.packageName) return@mapNotNull null
            val appInfo = try {
                pm.getApplicationInfo(pkg, 0)
            } catch (_: Exception) {
                return@mapNotNull null
            }
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val running = runningByPkg[pkg]
            val used = lastUsed[pkg] ?: 0L
            val recentlyUsed = used > 0L && now - used < 15 * 60 * 1000
            if (running == null && !recentlyUsed && !launchable.contains(pkg)) {
                return@mapNotNull null
            }
            AppProcess(
                packageName = pkg,
                label = pm.getApplicationLabel(appInfo).toString(),
                pid = running?.pid,
                lastUsedMs = used,
                importance = running?.importanceLabel ?: if (recentlyUsed) "Recent" else "Installed",
                isSystem = isSystem,
                canEnd = running != null || recentlyUsed
            )
        }.sortedWith(
            compareByDescending<AppProcess> { it.pid != null }
                .thenByDescending { it.lastUsedMs }
        )
    }

    fun endProcess(packageName: String): String {
        if (packageName == context.packageName) {
            return "Can't end this app from itself"
        }
        return try {
            activityManager.killBackgroundProcesses(packageName)
            "Asked Android to end background processes for $packageName.\nForeground apps may stay open — Android blocks force-stop without root."
        } catch (e: Exception) {
            "Failed: ${e.message}"
        }
    }

    private data class Running(
        val pid: Int,
        val importanceLabel: String
    )

    private fun runningProcessesByPackage(): Map<String, Running> {
        val map = mutableMapOf<String, Running>()
        val processes = try {
            activityManager.runningAppProcesses
        } catch (_: Exception) {
            emptyList()
        } ?: emptyList()

        for (proc in processes) {
            val pkgs = proc.pkgList ?: continue
            val label = when (proc.importance) {
                ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "Foreground"
                ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE -> "Foreground service"
                ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "Visible"
                ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "Service"
                ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED -> "Cached"
                else -> "Background"
            }
            for (pkg in pkgs) {
                val existing = map[pkg]
                if (existing == null || proc.pid != 0) {
                    map[pkg] = Running(proc.pid, label)
                }
            }
        }
        return map
    }

    private fun lastUsedByPackage(now: Long): Map<String, Long> {
        if (!hasUsageAccess()) return emptyMap()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            now - 6L * 60L * 60L * 1000L,
            now
        ) ?: return emptyMap()
        return stats
            .filter { it.lastTimeUsed > 0 }
            .associate { it.packageName to it.lastTimeUsed }
    }
}
