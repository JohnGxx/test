package com.john.youlan.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.john.youlan.model.AppEntry

object InstalledApps {
    fun loadLaunchableApps(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val activities = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        return activities.asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .mapNotNull { resolveInfo ->
                runCatching {
                    AppEntry(
                        packageName = resolveInfo.activityInfo.packageName,
                        label = resolveInfo.loadLabel(pm)?.toString()?.ifBlank { resolveInfo.activityInfo.packageName } ?: resolveInfo.activityInfo.packageName,
                        icon = resolveInfo.loadIcon(pm)
                    )
                }.getOrNull()
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun loadSelectedApps(context: Context, selectedPackages: Set<String>): List<AppEntry> {
        if (selectedPackages.isEmpty()) return emptyList()
        val allApps = loadLaunchableApps(context).associateBy { it.packageName }
        return selectedPackages.mapNotNull(allApps::get).sortedBy { it.label.lowercase() }
    }
}
