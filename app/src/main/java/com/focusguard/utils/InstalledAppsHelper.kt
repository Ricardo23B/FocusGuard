package com.focusguard.utils

import android.content.Context
import android.content.Intent

data class InstalledApp(val packageName: String, val label: String)

object InstalledAppsHelper {

    // Lista las apps lanzables sin necesitar QUERY_ALL_PACKAGES.
    // Funciona gracias al bloque <queries> del manifest.
    fun getLaunchableApps(context: Context): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val pm = context.packageManager
        return pm.queryIntentActivities(intent, 0)
            .asSequence()
            .map {
                InstalledApp(
                    packageName = it.activityInfo.packageName,
                    label = it.loadLabel(pm).toString()
                )
            }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
