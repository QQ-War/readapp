package com.readapp.media

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import java.io.File

object PlayerHolder {
    @Volatile
    private var player: ExoPlayer? = null
    @Volatile
    private var cache: SimpleCache? = null

    private const val MAX_CACHE_SIZE: Long = 100 * 1024 * 1024 // 100MB

    fun get(context: Context): ExoPlayer {
        return player ?: synchronized(this) {
            player ?: buildPlayer(context.applicationContext).also { player = it }
        }
    }

    fun getCache(context: Context): SimpleCache {
        return cache ?: synchronized(this) {
            cache ?: buildCache(context.applicationContext).also { cache = it }
        }
    }
    
    fun getUpstreamDataSourceFactory(context: Context): DataSource.Factory {
        return DefaultHttpDataSource.Factory()
    }

    fun getCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        return CacheDataSource.Factory()
            .setCache(getCache(context))
            .setUpstreamDataSourceFactory(getUpstreamDataSourceFactory(context))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    private fun buildCache(context: Context): SimpleCache {
        val cacheFolder = File(context.cacheDir, "tts_audio")
        val databaseProvider = StandaloneDatabaseProvider(context)
        return SimpleCache(cacheFolder, LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE), databaseProvider)
    }

    private fun buildPlayer(context: Context): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()
        
        return ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
}
