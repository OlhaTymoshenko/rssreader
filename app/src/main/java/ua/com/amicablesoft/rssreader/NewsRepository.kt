package ua.com.amicablesoft.rssreader

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by lapa on 25.11.16.
 */
class NewsRepository(ctx: Context) {
    private val TAG = NewsRepository::class.java.simpleName
    private val URL = "http://feeds.abcnews.com/abcnews/topstories"
    private val KEY__TIMESTAMP = "timestamp"
    private val client: OkHttpClient
    private val cachedDir: File
    private val sp: SharedPreferences
    private val parser: NewsXmlParser

    init {
        val loggingInterceptor = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger{
            private val TAG = "OkHttp"

            override fun log(message: String?) {
                if (message != null) {
                    Log.d(TAG, message)
                }
            }
        })
        loggingInterceptor.level = Level.HEADERS

        client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build()
        cachedDir = ctx.cacheDir
        sp = ctx.getSharedPreferences("last_feed_update", Context.MODE_PRIVATE)

        parser = NewsXmlParser()
    }

    fun fetchNews(invalidateCache:Boolean): Observable<List<News>> {
        return fetchNewsXml(invalidateCache)
                .map { newsXml ->
                    parser.parse(newsXml)
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }


    private fun fetchNewsXml(invalidateCache: Boolean): Observable<String> {
        if (invalidateCache) {
            return fetchNewsXmlRemote()
        }

        if (isCacheValid()) {
            val cachedResponse = getCachedResponse()
            if (cachedResponse != null) {
                return Observable.just(cachedResponse)
            }
            else {
                return fetchNewsXmlRemote()
            }
        }
        else {
            return fetchNewsXmlRemote()
        }
    }

    private fun isCacheValid(): Boolean {
        val cachedTimestamp = sp.getLong(KEY__TIMESTAMP, 0)
        val nowTimestamp = Date().time
        return (nowTimestamp - cachedTimestamp < TimeUnit.DAYS.toMillis(1))
    }

    private fun updateCachedTimestamp() {
        val nowTimestamp = Date().time
        sp.edit().putLong(KEY__TIMESTAMP, nowTimestamp).apply()
    }

    private fun fetchNewsXmlRemote(): Observable<String> {
        return Observable.create<Response> { subscriber ->
            val request = Request.Builder()
                    .url(URL)
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .build()
            try {
                val response = client.newCall(request).execute()
                subscriber.onNext(response)
                subscriber.onCompleted()
                response.close()
            }
            catch (ex: IOException) {
                subscriber.onError(ex)
            }
        }
                .map { response ->
                    response.body().string()
                }
                .doOnNext { response ->
                    cacheResponse(response)
                }

    }

    private fun cacheResponse(response: String) {
        val cacheFile = File(cachedDir, "news")
        val writer = FileWriter(cacheFile)
        writer.write(response)
        writer.close()
        updateCachedTimestamp()
    }

    private fun getCachedResponse(): String? {
        try {
            val cacheFile = File(cachedDir, "news")
            val reader = FileReader(cacheFile)
            val response = reader.readText()
            reader.close()
            return response
        }
        catch (ex: IOException) {
            Log.e(TAG, "Fail to read cached response", ex)
            return null
        }
    }
}