package com.example.youtubemusicdownloader.data

import com.example.youtubemusicdownloader.data.models.MusicDownloadData
import com.example.youtubemusicdownloader.data.models.SongPath
import com.example.youtubemusicdownloader.utils.log
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.io.*
import java.util.concurrent.TimeUnit

interface Api {
    @Streaming
    @GET
    fun loadMusic(@Url url: String): Single<ResponseBody>

    @GET("tests/index.php")
    fun getMusicData(@Query("url") ytUrl: String): Single<List<MusicDownloadData>>
}

class RemoteSource private constructor(){
    companion object {
        val default = RemoteSource()
    }

    private lateinit var api: Api

    init {
        createClient()
    }

    private fun createClient() {
        val client = OkHttpClient.Builder()
            .readTimeout(450, TimeUnit.SECONDS)
            .connectTimeout(100, TimeUnit.SECONDS)
            .addInterceptor {chain ->
                log("${chain.request()}")
                chain.request()
                    .newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .build()
                    .let { chain.proceed(it) }
            }

            .connectionSpecs(
                listOf(
                    ConnectionSpec.CLEARTEXT,
                    ConnectionSpec.MODERN_TLS
                )
            )
            .build()
        api = Retrofit.Builder()
            .baseUrl("http://82.146.47.250/ytd/")
            .addConverterFactory(MoshiConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(client)
            .build()
            .create(Api::class.java)
    }

    fun downloadMusicFromYoutubeUrl(url: String, filepath: String): Observable<Pair<MusicDownloadData, Int>>{
        return api.getMusicData(url)
            .subscribeOn(Schedulers.io())
            .map { it.first() }
            .doOnSuccess { log("success response: $it") }
            .doOnError { log("fetch music data error: $it") }
            .flatMapObservable { data ->
                loadingFile(data.url, SongPath(filepath, "/${data.title.replace("/", "_")}.mp3"))
                        .distinctUntilChanged()
                        .map { Pair(data, it)}
            }
    }

    private fun loadingFile(url: String, filePath: SongPath): Observable<Int>{
        return api.loadMusic(url)
            .subscribeOn(Schedulers.io())
            .flatMapObservable { responseBody ->
                Observable.create<Int> { emitter ->
                    writeResponseBodyToDisk(responseBody, filePath, emitter)
                }
            }
    }



    private fun writeResponseBodyToDisk(body: ResponseBody, path: SongPath, emitter: ObservableEmitter<Int>): Boolean {

        try {

            val futureStudioIconFile = File(path.path, path.name)//File(context.getExternalFilesDir(null).toString(), "/tempMusic")

            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null

            try {
                val fileReader = ByteArray(4096)

                val fileSize = body.contentLength()
                var fileSizeDownloaded: Long = 0

                inputStream = body.byteStream()
                outputStream = FileOutputStream(futureStudioIconFile)

                while (true) {
                    val read = inputStream!!.read(fileReader)

                    if (read == -1) {
                        break
                    }

                    outputStream.write(fileReader, 0, read)

                    fileSizeDownloaded += read.toLong()
                    val completePercent = (fileSizeDownloaded.toDouble() / fileSize.toDouble()) * 100
                    if (!emitter.isDisposed){
                        emitter.onNext(completePercent.toInt())
                    }  else {
                        futureStudioIconFile.delete()
                        break
                    }
                    //log(completePercent.toInt().toString())

                }

                outputStream.flush()

                return true
            } catch (e: IOException) {
                emitter.onError(e)
                return false
            } finally {
                inputStream?.close()
                outputStream?.close()
                emitter.onComplete()
            }
        } catch (e: IOException) {
            emitter.onError(e)
            return false
        }

    }

}