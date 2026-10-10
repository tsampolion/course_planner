package com.coursebook.pacer

import android.app.Application
import com.coursebook.pacer.data.PlanStore

/** The composition root. Dependencies are passed by constructor; there is no DI framework. */
class PacerApplication : Application() {
    val store: PlanStore by lazy { PlanStore(filesDir) }
}
