package com.taskmanager.app.process

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process

class ProcessRepository(private val context: Context) {

    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    private val protectedPackages = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.launcher",
        "com.android.launcher3",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.google.android.permissioncontroller",
        context.packageName
    )

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

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
        val launchable = launchablePackages(pm)

        val packages = launchable + runningByPkg.keys + lastUsed.keys

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
            val protectedPkg = pkg in protectedPackages || pkg.startsWith("com.android.")
            AppProcess(
                packageName = pkg,
                label = pm.getApplicationLabel(appInfo).toString(),
                pid = running?.pid,
                lastUsedMs = used,
                importance = running?.importanceLabel ?: if (recentlyUsed) "Recent" else "Installed",
                isSystem = isSystem,
                canEnd = !protectedPkg && (running != null || recentlyUsed),
                memoryMb = running?.pid?.let { memoryForPid(it) }
            )
        }.sortedWith(
            compareByDescending<AppProcess> { it.pid != null }
                .thenByDescending { it.memoryMb ?: -1 }
                .thenByDescending { it.lastUsedMs }
        )
    }

    fun endProcess(packageName: String): String {
        if (packageName == context.packageName) return "Can't end this app from itself"
        if (packageName in protectedPackages || packageName.startsWith("com.android.")) {
            return "Blocked: system process"
        }
        return try {
            activityManager.killBackgroundProcesses(packageName)
            "Ended background processes for ${packageName.substringAfterLast('.')}"
        } catch (e: Exception) {
            "Failed: ${e.message ?: "unknown error"}"
        }
    }

    private data class Running(
        val pid: Int,
        val importanceLabel: String
    )

    private fun launchablePackages(pm: PackageManager): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return resolved.map { it.activityInfo.packageName }.toSet()
    }

    private fun memoryForPid(pid: Int): Int? {
        return try {
            val info = activityManager.getProcessMemoryInfo(intArrayOf(pid))
            if (info.isEmpty()) null else (info[0].totalPss / 1024)
        } catch (_: Exception) {
            null
        }
    }

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
                map[pkg] = Running(proc.pid, label)
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
        return stats.filter { it.lastTimeUsed > 0 }.associate { it.packageName to it.lastTimeUsed }
    }
}
