package com.example.youtubemusicdownloader.utils

import android.content.Context
import androidx.multidex.BuildConfig
import androidx.multidex.MultiDexApplication

const val AppTag = "YMDownloader"
val DEBUG = BuildConfig.DEBUG

class XApplication: MultiDexApplication() {

    companion object {
        lateinit var sharedContext: Context
    }

    override fun onCreate() {
        super.onCreate()
        sharedContext = applicationContext
    }
}