package com.john.youlan.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.john.youlan.MainActivity
import com.john.youlan.service.YouLanAccessibilityService

object SystemState {
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val expected = ComponentName(context, YouLanAccessibilityService::class.java).flattenToString()
        return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    fun isDefaultLauncher(context: Context): Boolean {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(homeIntent, 0)
        return resolved?.activityInfo?.packageName == context.packageName && resolved.activityInfo?.name == MainActivity::class.java.name
    }
}
