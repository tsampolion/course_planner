package gr.edusuite.pacer.ui

/** A version as the teacher reads it: a last number of 0 is hidden, so 0.4.14.0 shows as 0.4.14 and 0.4.14.1 in full. */
fun shortVersion(version: String): String {
    val v = version.trim()
    val parts = v.split(".")
    return if (parts.size == 4 && parts[3] == "0") parts.take(3).joinToString(".") else v
}

/** The short version with the release's name after it, when it has one ("0.4.15 · Great Expectations"). */
fun releaseLabel(version: String, name: String?): String =
    shortVersion(version) + (name?.trim()?.takeIf { it.isNotEmpty() }?.let { " · $it" }.orEmpty())

/** The installed app's version name, such as 0.4.15.0 ("" if it can't be read). */
@androidx.compose.runtime.Composable
fun rememberVersionName(): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    return androidx.compose.runtime.remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
}
