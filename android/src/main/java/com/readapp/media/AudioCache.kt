package com.readapp.media

import android.util.LruCache

/**
 * A simple in-memory cache for TTS audio data.
 * The key is a unique identifier for a paragraph, e.g., "bookUrl/chapterIndex/paragraphIndex".
 * The value is the downloaded audio data.
 */
object AudioCache {
    private const val MAX_CACHE_SIZE = 20 * 1024 * 1024 // 20 MB

    private val cache = object : LruCache<String, ByteArray>(MAX_CACHE_SIZE) {
        override fun sizeOf(key: String, value: ByteArray): Int {
            return value.size
        }
    }

    fun put(key: String, data: ByteArray) {
        synchronized(this) {
            cache.put(key, data)
        }
    }

    fun get(key: String): ByteArray? {
        synchronized(this) {
            return cache.get(key)
        }
    }

    fun clear() {
        synchronized(this) {
            cache.evictAll()
        }
    }
}
