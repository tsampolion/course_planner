package gr.edusuite.pacer.update

/** The single public endpoint used for update checks. */
const val RELEASES_API = "https://api.github.com/repos/tsampolion/course_planner/releases/latest"
/** Only release downloads from this repository are accepted. */
const val APK_URL_PREFIX = "https://github.com/tsampolion/course_planner/releases/download/"
/** Only release pages from this repository are accepted. */
const val PAGE_URL_PREFIX = "https://github.com/tsampolion/course_planner/releases/"
/** The page shared with other teachers; it always shows the newest release. */
const val RELEASE_LATEST_URL = PAGE_URL_PREFIX + "latest"
/** Automatic checks are separated by one day. */
const val CHECK_INTERVAL_MS = 86_400_000L
/** Maximum number of nonempty release-note lines shown. */
const val NOTE_LINES = 3

/**
 * A numeric release version: three parts (`X.Y.Z`, up to 4.1.0) or four (`0.X.Y.Z`, from 0.4.1.1).
 * A three-part version compares as `[X, Y, Z, 0]` and a four-part one as itself, so every old
 * version with X >= 1 is newer than any `0.x.y.z`; the first four-part build is installed by hand.
 */
data class Version(val parts: List<Int>) : Comparable<Version> {
    init { require(parts.size == 3 || parts.size == 4) }

    override fun compareTo(other: Version): Int {
        val a = padded()
        val b = other.padded()
        for (i in a.indices) if (a[i] != b[i]) return a[i].compareTo(b[i])
        return 0
    }

    private fun padded(): List<Int> = if (parts.size == 3) parts + 0 else parts

    /** The parts joined by dots, as used in tags and asset names. */
    fun text(): String = parts.joinToString(".")
}

private val versionPattern = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:\\.(\\d+))?$")

/** Reads a bare version or lowercase-v tag of three or four parts, rejecting overflowing parts. */
fun parseVersion(s: String?): Version? {
    val groups = versionPattern.matchEntire(s?.trim() ?: return null)?.groupValues ?: return null
    val parts = groups.drop(1).filter { it.isNotEmpty() }.map { it.toIntOrNull() ?: return null }
    return Version(parts)
}

/** Whether latest is strictly newer. */
fun isNewer(latest: Version, installed: Version): Boolean = latest > installed

/** The text sent through the share sheet: the message, then the link on its own line. */
fun shareText(message: String, url: String = RELEASE_LATEST_URL): String = "$message\n$url"

/** Validated details of an available release. */
data class Update(val version: String, val notes: String, val apkUrl: String, val pageUrl: String)
/** Fields copied from GitHub's latest-release response. */
data class Release(val tag: String?, val body: String?, val pageUrl: String?, val assets: List<Pair<String, String>>)

/** Picks the exact release APK asset name. */
fun pickApk(assets: List<Pair<String, String>>, version: String): String? =
    assets.firstOrNull { it.first == "course-pacer-$version.apk" }?.second

/** Validates release metadata before any download link can be shown. */
fun toUpdate(release: Release): Update? {
    val v = parseVersion(release.tag) ?: return null
    val version = v.text()
    return storedUpdate(version, shortNotes(release.body), pickApk(release.assets, version), release.pageUrl)
}

/** Due after a day, on first launch, or after the clock moved backwards. */
fun isDue(lastCheck: Long, now: Long): Boolean = lastCheck <= 0 || now < lastCheck || now - lastCheck >= CHECK_INTERVAL_MS

/** A short plain-text preview of Markdown notes. */
fun shortNotes(markdown: String?): String = markdown.orEmpty().replace("\r\n", "\n").replace("\r", "\n")
    .lineSequence().map { line ->
        line.replace(Regex("!\\[[^\\]]*]\\([^)]*\\)"), "")
            .replace(Regex("\\[([^\\]]*)]\\([^)]*\\)"), "$1")
            .replace(Regex("^#+\\s*"), "")
            .replace(Regex("^\\s*([-*+]|\\d+\\.)\\s+"), "")
            .replace(Regex("[*_`]"), "")
            .replace(Regex("\\s+"), " ").trim()
    }.filter { it.isNotEmpty() }.take(NOTE_LINES).joinToString("\n")

/** Keeps a stored release only while it is newer than the installed app. */
fun pendingUpdate(stored: Update?, installed: String): Update? {
    val latest = parseVersion(stored?.version) ?: return null
    val current = parseVersion(installed) ?: return null
    return stored.takeIf { isNewer(latest, current) }
}

/** Validates stored strings just as strictly as freshly fetched links. */
fun storedUpdate(version: String?, notes: String?, apkUrl: String?, pageUrl: String?): Update? {
    val v = parseVersion(version) ?: return null
    if (apkUrl?.startsWith(APK_URL_PREFIX) != true || pageUrl?.startsWith(PAGE_URL_PREFIX) != true) return null
    return Update(v.text(), notes.orEmpty(), apkUrl, pageUrl)
}
