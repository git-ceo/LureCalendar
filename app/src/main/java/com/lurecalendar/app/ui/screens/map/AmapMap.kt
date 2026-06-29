package com.lurecalendar.app.ui.screens.map

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.LocationSource
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle

data class MapMarkerItem(
    val id: String,
    val latLng: LatLng,
    val title: String
)

@Composable
fun AmapMapView(
    modifier: Modifier = Modifier,
    markers: List<MapMarkerItem>,
    centerOn: LatLng?,
    centerTrigger: Long = 0L,
    myLocationEnabled: Boolean,
    mapType: Int = AMap.MAP_TYPE_NORMAL,
    onMapLongClick: (LatLng) -> Unit,
    onMarkerClick: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapInit = remember {
        runCatching {
            MapView(context).apply { onCreate(null) }
        }
    }
    val mapView = mapInit.getOrNull()
    val initError = mapInit.exceptionOrNull()
    if (mapView == null) {
        Text(text = "地图初始化失败：${initError?.message ?: "unknown"}")
        return
    }

    val aMap = remember(mapView) { mapView.map }

    // No-op (removed hasCentered to allow re-centering)

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(aMap, myLocationEnabled) {
        if (myLocationEnabled) {
            val locationSource = AmapLocationSource(context)
            aMap.setLocationSource(locationSource)
            aMap.isMyLocationEnabled = true
            aMap.uiSettings.isMyLocationButtonEnabled = false
            aMap.myLocationStyle = MyLocationStyle().apply {
                myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
            }
            onDispose {
                aMap.isMyLocationEnabled = false
                aMap.setLocationSource(null)
                locationSource.deactivate()
            }
        } else {
            aMap.isMyLocationEnabled = false
            aMap.setLocationSource(null)
            onDispose { }
        }
    }

    DisposableEffect(aMap) {
        val longClickListener = AMap.OnMapLongClickListener { latLng ->
            onMapLongClick(latLng)
        }
        val markerClickListener = AMap.OnMarkerClickListener { marker ->
            val id = marker.`object` as? String
            if (id != null) {
                onMarkerClick(id)
            }
            true
        }
        
        aMap.setOnMapLongClickListener(longClickListener)
        aMap.setOnMarkerClickListener(markerClickListener)
        
        onDispose {
            aMap.setOnMapLongClickListener(null)
            aMap.setOnMarkerClickListener(null)
        }
    }

    LaunchedEffect(markers) {
        aMap.clear(true)
        markers.forEach { item ->
            val marker = aMap.addMarker(MarkerOptions().position(item.latLng).title(item.title))
            marker.`object` = item.id
        }
    }

    LaunchedEffect(centerOn, centerTrigger) {
        val target = centerOn ?: return@LaunchedEffect
        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 16f))
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { view ->
            if (view.map.mapType != mapType) {
                view.map.mapType = mapType
            }
        }
    )
}

private class AmapLocationSource(
    private val context: android.content.Context
) : LocationSource, AMapLocationListener {
    private var listener: LocationSource.OnLocationChangedListener? = null
    private var client: AMapLocationClient? = null

    override fun activate(l: LocationSource.OnLocationChangedListener?) {
        listener = l
        if (client != null) return

        if (Looper.myLooper() == Looper.getMainLooper()) {
            initClient()
        } else {
            Handler(Looper.getMainLooper()).post { initClient() }
        }
    }

    private fun initClient() {
        if (client != null) return
        runCatching {
            val c = AMapLocationClient(context)
            val option = AMapLocationClientOption().apply {
                locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                isNeedAddress = false
                interval = 2000
                isMockEnable = false
            }
            c.setLocationOption(option)
            c.setLocationListener(this)
            c.startLocation()
            client = c
        }
    }

    override fun deactivate() {
        listener = null
        val c = client
        if (c != null) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                c.stopLocation()
                c.onDestroy()
            } else {
                Handler(Looper.getMainLooper()).post {
                    c.stopLocation()
                    c.onDestroy()
                }
            }
        }
        client = null
    }

    override fun onLocationChanged(loc: AMapLocation?) {
        if (loc == null) return
        if (loc.errorCode != 0) return
        listener?.onLocationChanged(loc)
    }
}
