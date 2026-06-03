package com.shortdrama.dracin

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.utils.newExtractorLink

data class DracinResponse(
    @JsonProperty("items") val items: List<DracinItem>? = null,
    @JsonProperty("data") val data: List<DracinItem>? = null,
    @JsonProperty("results") val results: List<DracinItem>? = null
)

data class DracinItem(
    @JsonProperty("id") val id: String? = null,
    @JsonProperty("book_id") val bookId: String? = null,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("image") val image: String? = null,
    @JsonProperty("poster") val poster: String? = null,
    @JsonProperty("description") val description: String? = null,
    @JsonProperty("overview") val overview: String? = null,
    @JsonProperty("episodes") val episodes: List<DracinEpisode>? = null,
    @JsonProperty("chapters") val chapters: List<DracinEpisode>? = null
)

data class DracinEpisode(
    @JsonProperty("ep") val ep: Int? = null,
    @JsonProperty("id") val id: String? = null,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("episode") val episode: Int? = null
)

data class DracinStream(
    @JsonProperty("videoUrl") val videoUrl: String? = null,
    @JsonProperty("url") val url: String? = null,
    @JsonProperty("qualityList") val qualityList: List<DracinQuality>? = null,
    @JsonProperty("qualities") val qualities: List<DracinQuality>? = null
)

data class DracinQuality(
    @JsonProperty("label") val label: String? = null,
    @JsonProperty("url") val url: String? = null,
    @JsonProperty("quality") val quality: String? = null
)

abstract class DracinBaseProvider : MainAPI() {
    override var mainUrl = "https://api.anichin.bio"
    private val apiKey = "TRIAL-ANICHIN-2026"
    override val hasQuickSearch = true
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.TvSeries, TvType.AsianDrama)

    abstract val sourceName: String

    private val headers get() = mapOf(
        "X-API-Key" to apiKey,
        "User-Agent" to "Mozilla/5.0"
    )

    override val mainPage = mainPageOf(
        "$mainUrl/$sourceName/trending" to "Trending",
        "$mainUrl/$sourceName/foryou?page=%d" to "For You"
    )

    private fun parseItems(jsonStr: String): List<DracinItem> {
        val resp = tryParseJson<DracinResponse>(jsonStr)
        return resp?.items ?: resp?.data ?: resp?.results ?: emptyList()
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (request.data.contains("foryou"))
            request.data.replace("%d", page.toString())
        else
            request.data

        val resp = app.get(url, headers = headers)
        val items = parseItems(resp.text)

        val home = items.mapNotNull { item ->
            item.toSearchResult()
        }

        return newHomePageResponse(
            HomePageList(name = request.name, list = home),
            hasNext = home.isNotEmpty()
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/$sourceName/search?query=$query"
        val resp = app.get(url, headers = headers)
        val items = parseItems(resp.text)
        return items.mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val id = url.substringAfterLast("?id=").ifEmpty { url.substringAfterLast("/") }
        val resp = app.get("$mainUrl/$sourceName/detail?id=$id", headers = headers)
        val detail = tryParseJson<DracinItem>(resp.text)

        val title = detail?.title ?: return null
        val image = detail?.image ?: detail?.poster
        val desc = detail?.description ?: detail?.overview
        val episodes = (detail?.episodes ?: detail?.chapters ?: emptyList()).mapIndexed { index, ep ->
            newEpisode("id=$id&ep=${ep.ep ?: ep.episode ?: (index + 1)}") {
                this.name = ep.title ?: "Episode ${ep.ep ?: ep.episode ?: (index + 1)}"
                this.episode = ep.ep ?: ep.episode ?: (index + 1)
            }
        }

        return newAnimeLoadResponse(title, url, TvType.TvSeries) {
            this.posterUrl = image
            this.plot = desc
            addEpisodes(DubStatus.Subbed, episodes)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val params = data.split("&").associate {
            val parts = it.split("=", limit = 2)
            parts[0] to (parts.getOrNull(1) ?: "")
        }
        val id = params["id"] ?: return false
        val ep = params["ep"] ?: "1"

        val resp = app.get("$mainUrl/$sourceName/episode?id=$id&ep=$ep", headers = headers)
        val stream = tryParseJson<DracinStream>(resp.text)

        val videos = mutableListOf<Pair<String?, String?>>()

        if (stream != null) {
            val qualityList = stream.qualityList ?: stream.qualities ?: emptyList()
            if (qualityList.isNotEmpty()) {
                for (q in qualityList) {
                    val qUrl = q.url ?: continue
                    val qLabel = q.label ?: q.quality ?: "Unknown"
                    videos.add(qLabel to qUrl)
                }
            } else {
                val videoUrl = stream.videoUrl ?: stream.url
                if (videoUrl != null) videos.add(null to videoUrl)
            }
        }

        if (videos.isEmpty()) {
            val fallback = tryParseJson<Map<String, Any>>(resp.text)
            if (fallback != null) {
                for ((key, value) in fallback) {
                    if (value is String && (value.startsWith("http://") || value.startsWith("https://")) &&
                        (value.contains(".mp4") || value.contains(".m3u8"))
                    ) {
                        videos.add(key to value)
                    }
                }
            }
        }

        for ((quality, videoUrl) in videos) {
            if (videoUrl != null) {
                val qualityNum = if (quality != null) getQualityFromName(quality) else -1
                callback.invoke(
                    newExtractorLink(
                        sourceName,
                        "${name} - ${quality ?: "Video"}",
                        videoUrl,
                        "",
                        qualityNum,
                        videoUrl.contains(".m3u8")
                    )
                )
            }
        }

        return videos.isNotEmpty()
    }

    private fun DracinItem.toSearchResult(): SearchResponse? {
        val itemId = id ?: bookId ?: return null
        val itemTitle = title ?: return null
        val itemImage = image ?: poster
        return newSearchResponse(itemTitle, "?id=$itemId") {
            this.posterUrl = itemImage
        }
    }
}

class DramaboxProvider : DracinBaseProvider() {
    override var name = "DramaBox"
    override val sourceName = "dramabox"
}

class ReelshortProvider : DracinBaseProvider() {
    override var name = "ReelShort"
    override val sourceName = "reelshort"
}

class FlickreelsProvider : DracinBaseProvider() {
    override var name = "FlickReels"
    override val sourceName = "flickreels"
}

class DramawaveProvider : DracinBaseProvider() {
    override var name = "DramaWave"
    override val sourceName = "dramawave"
}

class GoodshortProvider : DracinBaseProvider() {
    override var name = "GoodShort"
    override val sourceName = "goodshort"
}

class NetshortProvider : DracinBaseProvider() {
    override var name = "NetShort"
    override val sourceName = "netshort"
}

class IdramaProvider : DracinBaseProvider() {
    override var name = "iDrama"
    override val sourceName = "idrama"
}

class StardusttvProvider : DracinBaseProvider() {
    override var name = "StardustTV"
    override val sourceName = "stardusttv"
}

class DramabiteProvider : DracinBaseProvider() {
    override var name = "DramaBite"
    override val sourceName = "dramabite"
}

class ShortmaxProvider : DracinBaseProvider() {
    override var name = "ShortMax"
    override val sourceName = "shortmax"
}
