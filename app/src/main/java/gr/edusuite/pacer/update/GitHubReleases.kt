package gr.edusuite.pacer.update

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/** One bounded GET to this repository's latest public release; call on Dispatchers.IO. */
class GitHubReleases(private val installed: String) : ReleaseSource {
    override fun latest(): Release? {
        var connection: HttpURLConnection? = null
        return try {
            val request = URL(RELEASES_API).openConnection() as HttpURLConnection
            connection = request
            request.connectTimeout = 5000
            request.readTimeout = 10000
            request.requestMethod = "GET"
            request.instanceFollowRedirects = true
            request.setRequestProperty("Accept", "application/vnd.github+json")
            request.setRequestProperty("User-Agent", "CoursePacer/$installed")
            if (request.responseCode != 200) return null
            val bytes = request.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > 512 * 1024) return null
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            readRelease(bytes.toString(Charsets.UTF_8))
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}

/** Copies platform JSON fields into plain data; validation belongs to the model. */
internal fun readRelease(json: String): Release? = runCatching {
    val root = JSONObject(json)
    fun JSONObject.text(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key, "").takeIf { it.isNotEmpty() }
    val items = root.optJSONArray("assets")
    val assets = buildList {
        if (items != null) for (index in 0 until items.length()) {
            val asset = items.optJSONObject(index) ?: continue
            val name = asset.text("name") ?: continue
            val url = asset.text("browser_download_url") ?: continue
            add(name to url)
        }
    }
    Release(root.text("tag_name"), root.text("body"), root.text("html_url"), assets)
}.getOrNull()
