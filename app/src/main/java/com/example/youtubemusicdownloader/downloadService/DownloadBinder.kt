package com.example.youtubemusicdownloader.downloadService

import android.os.Binder
import com.example.youtubemusicdownloader.data.models.DownloadDataState
import com.example.youtubemusicdownloader.data.models.MusicDownloadData
import io.reactivex.Notification
import io.reactivex.Observable

class DownloadBinder(private val service: DownloadService,
                     val downloadStatus: Observable<Notification<DownloadDataState>>
): Binder(){

    fun cancelDownloading(){
        service.stopLoading()
    }

}