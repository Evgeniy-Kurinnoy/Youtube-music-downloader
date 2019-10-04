package com.example.youtubemusicdownloader.musicDownload

import android.Manifest
import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.example.youtubemusicdownloader.*
import com.example.youtubemusicdownloader.utils.log
import com.example.youtubemusicdownloader.data.Defaults
import com.example.youtubemusicdownloader.data.models.MusicDownloadData
import com.example.youtubemusicdownloader.data.RemoteSource
import com.example.youtubemusicdownloader.data.models.DownloadDataState
import com.example.youtubemusicdownloader.downloadService.DownloadBinder
import com.example.youtubemusicdownloader.downloadService.DownloadService
import io.reactivex.Notification
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit


class MusicDownloadViewModel(application: Application): AndroidViewModel(application) {

    class CancelException: Throwable()
    class BackException: Throwable()

    data class Input (
        val url: Observable<String>,
        val button: Observable<Unit>
    )

    data class Output(
        val progress: Observable<Int>,
        val startLoading: Observable<MusicDownloadData>,
        val error: Observable<Throwable>,
        val state: Observable<MusicDownloadActivity.State>
    )

    var currentState: MusicDownloadActivity.State = MusicDownloadActivity.State.default
        private set

    private val stateSubject = BehaviorSubject.create<MusicDownloadActivity.State>()

    private lateinit var binder: DownloadBinder

    private var disposable: Disposable? = null

    fun transform(input: Input, binder: DownloadBinder): Output {
        this.binder = binder

        val loading = binder.downloadStatus
            .doOnNext {
                val state = when {
                    it.isOnError -> MusicDownloadActivity.State.default
                    it.isOnComplete -> MusicDownloadActivity.State.completed
                    else -> MusicDownloadActivity.State.loading
                }
                stateSubject.onNext(state)
            }

        disposable = input.button
            .withLatestFrom(input.url)
            .map { it.second }
            .doOnNext { log("input url: $it") }
            .filter { !(it.isEmpty() && currentState == MusicDownloadActivity.State.default) }
            .subscribe {
                loadFile(it)
            }


        val progress = loading
            .filter { it.isOnNext }
            .map { it.value!!.progress }
            .throttleWithTimeout(500, TimeUnit.MILLISECONDS)

        val startLoading = loading
            .filter { it.isOnNext }
            .filter { it.value!!.musicData != null }
            .map { it.value!!.musicData!! }
            .distinctUntilChanged()
            .doOnEach { log("start loading: " + it.toString()) }

        val error = loading
            .filter { it.isOnError && it.error !is CancelException && it.error !is BackException }
            .map { it.error!! }
            .doOnEach { log("error: " + it.toString()) }

        val state = stateSubject
            .distinctUntilChanged()
            .filter { it != currentState }
            .doOnNext { currentState = it }
            .doOnDispose { disposable?.dispose() }

        return Output(
            progress.observeOn(AndroidSchedulers.mainThread()),
            startLoading.observeOn(AndroidSchedulers.mainThread()),
            error.observeOn(AndroidSchedulers.mainThread()),
            state.observeOn(AndroidSchedulers.mainThread())
        )
    }

    private fun loadFile(url: String){
        when (currentState) {
            MusicDownloadActivity.State.loading -> {
                binder.cancelDownloading()
                stateSubject.onNext(MusicDownloadActivity.State.default)
            }
            MusicDownloadActivity.State.completed -> {
                stateSubject.onNext(MusicDownloadActivity.State.default)
            }
            else -> {
                stateSubject.onNext(MusicDownloadActivity.State.preload)
                val intent = Intent(getApplication(), DownloadService::class.java)
                intent.putExtra("url", url)
                getApplication<Application>().startService(intent)
            }
        }
    }
}