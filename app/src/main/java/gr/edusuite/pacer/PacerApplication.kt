package gr.edusuite.pacer

import android.app.Application
import android.content.pm.ApplicationInfo
import gr.edusuite.pacer.update.GitHubReleases
import gr.edusuite.pacer.update.UpdateChecker
import gr.edusuite.pacer.data.PlanStore
import gr.edusuite.pacer.data.SettingsStore

/** The composition root. Dependencies are passed by constructor; there is no DI framework. */
class PacerApplication : Application() {
    val store: PlanStore by lazy { PlanStore(filesDir) }
    val settings: SettingsStore by lazy { SettingsStore(this) }
    val updates: UpdateChecker by lazy {
        val installed = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull().orEmpty()
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        UpdateChecker(GitHubReleases(installed), installed, enabled = !debuggable && installed.isNotEmpty())
    }
}
