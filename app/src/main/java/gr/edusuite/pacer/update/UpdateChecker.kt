package gr.edusuite.pacer.update

import gr.edusuite.pacer.model.Settings

/** Where the recent releases come from, newest first; null means the request failed. */
fun interface ReleaseSource { fun releases(): List<Release>? }

/** One check's outcome. */
sealed interface Fetched {
    /** A newer release with a trusted APK. */
    data class Found(val update: Update) : Fetched
    /** A successful check with nothing newer to install. */
    data object UpToDate : Fetched
    /** A request that did not complete successfully. */
    data object Failed : Fetched
}

/** Pure update decisions; the caller owns threading and persistence. */
class UpdateChecker(
    private val source: ReleaseSource,
    val installed: String,
    val enabled: Boolean,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** Drops a cached release when installed caught up or checks are disabled. */
    fun atLaunch(settings: Settings): Settings =
        if (pending(settings) == null) settings.copy(availableUpdate = null) else settings

    /** Whether launch should check now. */
    fun shouldAutoCheck(settings: Settings): Boolean =
        enabled && settings.autoUpdateCheck && isDue(settings.lastUpdateCheck, now())

    /** The newer cached release, hidden when checking is disabled. */
    fun pending(settings: Settings): Update? =
        if (enabled) pendingUpdate(settings.availableUpdate, installed) else null

    /** Blocking; call on Dispatchers.IO. Request failures never escape. */
    fun fetch(): Fetched {
        if (!enabled) return Fetched.Failed
        val releases = runCatching { source.releases() }.getOrNull() ?: return Fetched.Failed
        val update = pendingUpdate(toUpdate(releases, installed), installed) ?: return Fetched.UpToDate
        return Fetched.Found(update)
    }

    /** Applies a result to current preferences, preserving unrelated in-flight edits. */
    fun applied(settings: Settings, fetched: Fetched): Settings = when (fetched) {
        is Fetched.Found -> settings.copy(availableUpdate = fetched.update, lastUpdateCheck = now())
        Fetched.UpToDate -> settings.copy(availableUpdate = null, lastUpdateCheck = now())
        Fetched.Failed -> settings
    }
}
