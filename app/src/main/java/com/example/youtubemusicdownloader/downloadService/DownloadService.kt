package com.example.youtubemusicdownloader.downloadService

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.example.youtubemusicdownloader.data.Defaults
import com.example.youtubemusicdownloader.data.RemoteSource
import com.example.youtubemusicdownloader.data.models.DownloadDataState
import com.example.youtubemusicdownloader.data.models.SongPath
import com.example.youtubemusicdownloader.utils.log
import io.reactivex.Notification
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class DownloadService : Service() {
    private var disposable: Disposable? = null
    private val downloadSubject = BehaviorSubject.create<Notification<DownloadDataState>>()
    private var notification: DownloadNotification? = null

    override fun onCreate() {
        super.onCreate()
        notification = DownloadNotification(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        log("on start command")
        val url = intent?.getStringExtra("url")

        if (url.isNullOrEmpty() && disposable == null){
            clearAndStop()
        } else if (disposable == null){
            downloadMusic(url!!)
        } else {
            log("download is running, task rejected")
        }

        return START_NOT_STICKY
    }


    override fun onBind(intent: Intent): IBinder {
        log("bind service")
        return DownloadBinder(this, downloadSubject.doOnEach { log("subject item: $it") })
    }

    private fun downloadMusic(url: String){
        disposable?.dispose()
        disposable = RemoteSource.default.downloadMusicFromYoutubeUrl(url, Defaults.savePath)
            .map { DownloadDataState(it.first, it.second) }
            .doOnSubscribe {
                notification?.showNotification()
            }
            .doOnNext {
                log("on next: ${it.progress}")
                notification?.setDescription(it.musicData!!.title)
                notification?.setProgress(it.progress)
            }
            .doOnDispose { notification?.cancelNotification() }
            .doOnError{ notification?.cancelNotification() }
            .materialize()
            .subscribe({
                downloadSubject.onNext(it)
                if (it.isOnComplete || it.isOnError){
                    clearAndStop()
                }
            }, {
                log("error: $it")
                downloadSubject.onError(it)
                clearAndStop()
            }, {
                downloadSubject.onComplete()
                clearAndStop()
            }, {
                downloadSubject.onSubscribe(it)
            })
    }

    override fun onDestroy() {
        log("destroy service")
        super.onDestroy()
        clearAndStop()
    }

    fun stopLoading(){
        clearAndStop()
    }

    private fun clearAndStop(){
        log("stopped service")
        disposable?.dispose()
        disposable = null
//        val clearValue = downloadSubject.value?.copy(progress = 0, musicData = null, musicDataLoaded = false)
//        if (clearValue != null){
//            downloadSubject.onNext(clearValue)
//        }

        stopSelf()
    }

}

