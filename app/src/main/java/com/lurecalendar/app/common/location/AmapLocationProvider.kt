package com.lurecalendar.app.common.location

import android.content.Context
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val city: String?,
    val district: String?
)

@Singleton
class AmapLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun getOnceLocation(): DeviceLocation = withContext(Dispatchers.Main) {
        if (com.lurecalendar.app.BuildConfig.AMAP_API_KEY.isBlank()) {
            throw IllegalStateException("未配置高德 AMAP_API_KEY")
        }
        suspendCancellableCoroutine { cont ->
            val client = AMapLocationClient(context)
            val option = AMapLocationClientOption().apply {
                isOnceLocation = true
                isNeedAddress = true
                locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                isMockEnable = false
            }
            client.setLocationOption(option)
            val listener = AMapLocationListener { loc: AMapLocation? ->
                if (loc == null) {
                    client.onDestroy()
                    if (cont.isActive) cont.resumeWithException(IllegalStateException("定位失败"))
                    return@AMapLocationListener
                }
                if (loc.errorCode != 0) {
                    val msg = loc.errorInfo ?: "定位失败(${loc.errorCode})"
                    client.onDestroy()
                    if (cont.isActive) cont.resumeWithException(IllegalStateException(msg))
                    return@AMapLocationListener
                }
                val city = loc.city ?: loc.province
                val district = loc.district ?: loc.adCode
                val result = DeviceLocation(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    city = city,
                    district = district
                )
                client.onDestroy()
                if (cont.isActive) cont.resume(result)
            }
            client.setLocationListener(listener)
            client.startLocation()
            cont.invokeOnCancellation {
                client.stopLocation()
                client.onDestroy()
            }
        }
    }
}
