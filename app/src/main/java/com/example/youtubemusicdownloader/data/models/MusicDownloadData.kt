package com.example.youtubemusicdownloader.data.models


import com.squareup.moshi.Json

data class MusicDownloadData(
    val expire: Int,
    val img: List<String>,
    val itag: Int,
    val quality: String,
    val size: String,
    val title: String,
    val type: List<String>,
    val url: String
) {
    override fun toString(): String {
        return title
    }
}