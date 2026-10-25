package gr.edusuite.pacer.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.delay

/** Milliseconds from [now] to the next midnight: how long a visible screen waits before it asks for the date again. */
fun nextMidnightDelayMillis(now: LocalDateTime): Long =
    Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis()

/** The day every screen reads, kept by [TodayProvider]. */
private val LocalToday = compositionLocalOf { LocalDate.now() }

/** Today's date. It changes when the app resumes, the clock passes midnight, or the date or time zone is changed. */
@Composable
fun rememberToday(): LocalDate = LocalToday.current

/**
 * Gives [content] a current "today" from [vm]. The view model re-reads the date when the activity resumes
 * (`MainActivity.onResume`), when the system says the date, time or time zone changed, and at midnight while the app is open.
 */
@Composable
fun TodayProvider(vm: PacerViewModel, content: @Composable () -> Unit) {
    val today by vm.today.collectAsState()
    val context = LocalContext.current
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = vm.refreshToday()
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else context.registerReceiver(receiver, filter)
        onDispose { context.unregisterReceiver(receiver) }
    }
    // A timer sleeps through a locked phone's deep sleep; the resume and broadcast paths above cover that.
    LaunchedEffect(Unit) {
        while (true) {
            delay(nextMidnightDelayMillis(LocalDateTime.now()) + 1_000)
            vm.refreshToday()
        }
    }
    CompositionLocalProvider(LocalToday provides today, content = content)
}
