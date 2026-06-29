package com.lurecalendar.app.data.mapper

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonListAdapter @Inject constructor(
    moshi: Moshi
) {
    private val adapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )

    fun toJson(value: List<String>): String {
        return adapter.toJson(value)
    }

    fun fromJson(json: String): List<String> {
        return adapter.fromJson(json).orEmpty()
    }
}

