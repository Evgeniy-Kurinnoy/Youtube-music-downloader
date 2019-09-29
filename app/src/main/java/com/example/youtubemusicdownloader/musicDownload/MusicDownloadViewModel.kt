package com.example.youtubemusicdownloader.musicDownload

import android.Manifest
import com.example.youtubemusicdownloader.*
import com.example.youtubemusicdownloader.utils.log
import com.example.youtubemusicdownloader.data.Defaults
import com.example.youtubemusicdownloader.data.models.MusicDownloadData
import com.example.youtubemusicdownloader.data.RemoteSource
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.withLatestFrom
import java.util.concurrent.TimeUnit


class MusicDownloadViewModel(val activity: BaseActivity) {

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

    fun transform(input: Input): Output {
        val loading = input.button
            .withLatestFrom(input.url)
            .map { it.second }
            .doOnNext { log("input url: $it") }
            .filter { it.isNotEmpty() }
            .switchMap {
                loadFile(it).materialize()
            }
            .share()

        val progress = loading
            .filter { it.isOnNext }
            .map { it.value!!.second }
            .throttleWithTimeout(500, TimeUnit.MILLISECONDS)
            .doOnEach { log("progress: " + it.toString()) }

        val startLoading = loading
            .filter { it.isOnNext }
            .map { it.value!!.first }
            .distinctUntilChanged()
            .doOnEach { log("start loading: " + it.toString()) }

        val error = loading
            .filter { it.isOnError && it.error !is CancelException && it.error !is BackException }
            .map { it.error!! }
            .doOnEach { log("error: " + it.toString()) }

        val state = loading
            .map {
                when {
                    it.isOnError -> MusicDownloadActivity.State.default
                    it.isOnComplete -> MusicDownloadActivity.State.completed
                    else -> MusicDownloadActivity.State.loading
                }
            }
            .distinctUntilChanged()
            .filter { it != currentState }
            .doOnNext {
                currentState = it
            }
            .doOnEach { log("state: " + it.toString()) }

        return Output(
            progress.observeOn(AndroidSchedulers.mainThread()),
            startLoading.observeOn(AndroidSchedulers.mainThread()),
            error.observeOn(AndroidSchedulers.mainThread()),
            state.observeOn(AndroidSchedulers.mainThread())
        )
    }

    private fun loadFile(url: String): Observable<Pair<MusicDownloadData, Int>>{
        return when (currentState) {
            MusicDownloadActivity.State.loading ->  Observable.error(CancelException())
            MusicDownloadActivity.State.completed ->  Observable.error(BackException())
            else -> checkPermission()
                .doOnSuccess { log("check permission: $it") }
                .flatMapObservable {
                    if (!it)
                        throw SecurityException("need accept permission")
                    RemoteSource.default.downloadMusicFromYoutubeUrl(url, Defaults.savePath)

                }
                .doOnEach { log(it.toString()) }
                .doOnDispose { log("loading canceled") }
        }
    }

    private fun checkPermission(): Single<Boolean>{
        return Single.zip(listOf(activity.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            activity.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE))) {
            @Suppress("UNCHECKED_CAST")
            it[0] as Boolean && it[1] as Boolean
        }
    }
}