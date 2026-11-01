package gr.edusuite.pacer.update

/** The single public endpoint used for update checks: the 30 newest releases of this repository. */
const val RELEASES_API = "https://api.github.com/repos/tsampolion/course_planner/releases?per_page=30"
/** Only release downloads from this repository are accepted. */
const val APK_URL_PREFIX = "https://github.com/tsampolion/course_planner/releases/download/"
/** Only release pages from this repository are accepted. */
const val PAGE_URL_PREFIX = "https://github.com/tsampolion/course_planner/releases/"
/** The page shared with other teachers; it always shows the newest release. */
const val RELEASE_LATEST_URL = PAGE_URL_PREFIX + "latest"
/** Automatic checks are separated by one day. */
const val CHECK_INTERVAL_MS = 86_400_000L
/** Maximum number of release-note bullets shown. */
const val NOTE_LINES = 4

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
data class Release(
    val tag: String?, val body: String?, val pageUrl: String?, val assets: List<Pair<String, String>>,
    val prerelease: Boolean = false, val draft: Boolean = false,
)

/** Picks the exact release APK asset name. */
fun pickApk(assets: List<Pair<String, String>>, version: String): String? =
    assets.firstOrNull { it.first == "course-pacer-$version.apk" }?.second

/** Validates release metadata before any download link can be shown. */
fun toUpdate(release: Release): Update? {
    val v = parseVersion(release.tag) ?: return null
    val version = v.text()
    return storedUpdate(version, shortNotes(release.body), pickApk(release.assets, version), release.pageUrl)
}

/** The first release whose changelog bullets are written for teachers; older notes are never shown. */
val NOTES_FROM = Version(listOf(0, 4, 14, 0))

/** How big a release is, from the version part that changed: A (0), B (1) or C (2); lower is bigger. */
private fun tier(from: Version, to: Version): Int {
    val a = from.parts.let { if (it.size == 3) it + 0 else it }
    return (1..3).firstOrNull { a[it] != to.parts[it] }?.minus(1) ?: 2
}

/** `v0.X.Y.Z` releases only: not a draft or prerelease, and the old three-part tags are left out. */
private fun fourPart(release: Release): Version? =
    parseVersion(release.tag)?.takeIf { it.parts.size == 4 && it.parts[0] == 0 && !release.prerelease && !release.draft }

/**
 * The update shown to a teacher on [installed]: the newest release with a trusted APK, and notes of up to
 * [NOTE_LINES] bullets from every release since [installed] that has teacher-written notes. The first bullet is
 * the lead bullet of the biggest release (A, then B, then C; the newest on a tie); the rest are the next biggest
 * releases' lead bullets, then their second bullets, and so on. A release's size is judged against the one before it.
 */
fun toUpdate(releases: List<Release>, installed: String): Update? {
    val current = parseVersion(installed) ?: return null
    val newer = releases.mapNotNull { r -> fourPart(r)?.let { it to r } }
        .filter { isNewer(it.first, current) }.sortedBy { it.first }
    val newest = newer.lastOrNull { toUpdate(it.second) != null } ?: return null
    val base = toUpdate(newest.second)!!
    val byBiggest = newer.mapIndexed { i, (v, r) -> Triple(v, r, tier(if (i == 0) current else newer[i - 1].first, v)) }
        .filter { it.first >= NOTES_FROM }.sortedWith(compareBy<Triple<Version, Release, Int>> { it.third }.thenByDescending { it.first })
    val bullets = byBiggest.map { noteBullets(it.second.body) }
    val picked = LinkedHashSet<String>()
    var round = 0
    while (picked.size < NOTE_LINES && bullets.any { round < it.size }) {
        for (list in bullets) if (round < list.size && picked.size < NOTE_LINES) picked.add(list[round])
        round++
    }
    return base.copy(notes = picked.joinToString("\n"))
}

/** Due after a day, on first launch, or after the clock moved backwards. */
fun isDue(lastCheck: Long, now: Long): Boolean = lastCheck <= 0 || now < lastCheck || now - lastCheck >= CHECK_INTERVAL_MS

/**
 * The release notes as a teacher reads them: only the changelog's bullet lines, as plain text, one per line.
 * Headings, prose, the build's own lines and any bullet with a code span (developer wording) are left out.
 */
fun shortNotes(markdown: String?): String = noteBullets(markdown).take(NOTE_LINES).joinToString("\n")

/** Every plain bullet of a release's notes, in order. */
fun noteBullets(markdown: String?): List<String> = markdown.orEmpty().replace("\r\n", "\n").replace("\r", "\n")
    .lineSequence().map { it.trim() }
    .filter { it.matches(Regex("^([-*+])\\s+\\S.*")) && '`' !in it }
    .map { line ->
        line.replace(Regex("!\\[[^\\]]*]\\([^)]*\\)"), "")
            .replace(Regex("\\[([^\\]]*)]\\([^)]*\\)"), "$1")
            .replace(Regex("^([-*+])\\s+"), "")
            .replace(Regex("[*_]"), "")
            .replace(Regex("\\s+"), " ").trim()
    }.filter { it.isNotEmpty() }.toList()

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
