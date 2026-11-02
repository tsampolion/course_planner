package gr.edusuite.pacer.ui

import gr.edusuite.pacer.model.Features
import gr.edusuite.pacer.model.Module

/** One row of Settings › Features: a module and whether the teacher has turned it on. Saved data does not count as on. */
data class ModuleRow(val module: Module, val on: Boolean)

fun moduleRows(features: Features): List<ModuleRow> = Module.entries.map { ModuleRow(it, it in features.on) }

fun onCount(features: Features): Int = Module.entries.count { it in features.on }
