package com.lurecalendar.app.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeoLookupResponse(
    @Json(name = "code") val code: String,
    @Json(name = "location") val location: List<GeoLocation>?
)

@JsonClass(generateAdapter = true)
data class GeoLocation(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "adm1") val adm1: String?,
    @Json(name = "adm2") val adm2: String?,
    @Json(name = "lat") val lat: String?,
    @Json(name = "lon") val lon: String?
)

