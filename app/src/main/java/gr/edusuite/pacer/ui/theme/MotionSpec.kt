package gr.edusuite.pacer.ui.theme

import kotlin.math.roundToInt

/** Shared timings for group work and the following homework/units parts. Pure Kotlin for reduced-motion tests. */
object MotionSpec {
    enum class Step(val millis: Int) {
        PRESS(140), TICK(350), PACE(800), ROW(320), STAGGER(70), LONG_PRESS(400), MENU(240),
        SEGMENT(300), SHEET(320), SCRIM(240), REVISION(800), CALENDAR(300),
    }

    /** Undo's five seconds is a notification lifetime, not a transition duration. */
    const val UNDO_MILLIS = 5_000L

    const val ROW_RISE_PX = 14f
    const val CALENDAR_SHIFT_PX = 24

    fun entranceDelay(index: Int, alreadyShown: Boolean, scale: Float): Long =
        if (alreadyShown || scale <= 0f || !scale.isFinite()) 0L
        else index.coerceIn(0, 6).toLong() * duration(Step.STAGGER, scale)

    fun duration(step: Step, scale: Float): Int =
        if (!scale.isFinite() || scale <= 0f) 0 else (step.millis * scale).roundToInt()
}
