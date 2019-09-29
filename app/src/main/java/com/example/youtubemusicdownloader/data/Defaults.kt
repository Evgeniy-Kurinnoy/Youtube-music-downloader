package com.example.youtubemusicdownloader.data

import android.content.Context
import com.example.youtubemusicdownloader.utils.XApplication

class Defaults {
    companion object {
        private val pref = XApplication.sharedContext.getSharedPreferences("data", Context.MODE_PRIVATE)

        private const val defaultSavePath = "/storage/emulated/0/Download"
        var savePath: String
            get() = pref.getString("savePath", defaultSavePath) ?: defaultSavePath
            set(value) = pref.edit().putString("savePath", if (value.isEmpty()) defaultSavePath else value).apply()
    }
}