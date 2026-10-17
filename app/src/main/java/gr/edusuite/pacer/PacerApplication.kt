package gr.edusuite.pacer

import android.app.Application
import gr.edusuite.pacer.data.PlanStore
import gr.edusuite.pacer.data.SettingsStore

/** The composition root. Dependencies are passed by constructor; there is no DI framework. */
class PacerApplication : Application() {
    val store: PlanStore by lazy { PlanStore(filesDir) }
    val settings: SettingsStore by lazy { SettingsStore(this) }
}
