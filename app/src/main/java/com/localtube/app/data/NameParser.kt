package com.localtube.app.data

/** Pulls a clean title, year and IMDb id out of a file/folder name. */
object NameParser {
    private val imdbRegex = Regex("tt\\d{7,8}")
    private val yearRegex = Regex("(19|20)\\d{2}")
    private val bracketRegex = Regex("[\\[(][^\\])]*[\\])]")
    private val tagRegex = Regex(
        "(?i)\\b(1080p|720p|480p|2160p|4k|x264|x265|h264|h265|hevc|web-?dl|webrip|" +
            "bluray|bdrip|hdrip|dvdrip|aac|ac3|hdtv|repack|proper|remux|dts)\\b"
    )
    private val spaceRegex = Regex("\\s+")

    fun imdbId(name: String): String? = imdbRegex.find(name)?.value

    fun year(name: String): String? = yearRegex.find(name)?.value

    fun cleanTitle(name: String): String {
        var s = name
        s = imdbRegex.replace(s, " ")
        s = bracketRegex.replace(s, " ")
        s = s.replace('_', ' ').replace('.', ' ')
        s = tagRegex.replace(s, " ")
        s = spaceRegex.replace(s, " ").trim()
        return s.ifBlank { name.trim() }
    }
}
