package com.lurecalendar.app.ui.screens.catchrecord

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurecalendar.app.domain.model.CatchRecord
import com.lurecalendar.app.domain.model.FishingSpot
import com.lurecalendar.app.domain.repository.CatchRecordRepository
import com.lurecalendar.app.domain.repository.FishingSpotRepository
import com.lurecalendar.app.domain.repository.WeatherRepository
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import kotlin.coroutines.cancellation.CancellationException

data class CatchFormUiState(
    val spotId: String? = null,
    val spot: FishingSpot? = null,
    val allSpots: List<FishingSpot> = emptyList(),
    val showSpotPicker: Boolean = false,
    val species: String = "",
    val count: String = "1",
    val lengthCm: String = "",
    val weightG: String = "",
    val bait: String = "",
    val rod: String = "",
    val river: String = "",
    val city: String = "",
    val locationDetail: String = "",
    val note: String = "",
    val catchTime: Long = System.currentTimeMillis(),
    val photoUris: List<String> = emptyList(),
    val lureType: String = "",
    val rigType: String = "",
    val structureZone: String = "",
    val waterClarity: String = "",
    val windShoreRelation: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class CatchFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catchRecordRepository: CatchRecordRepository,
    private val fishingSpotRepository: FishingSpotRepository,
    private val weatherRepository: WeatherRepository
) : ViewModel() {
    private val _state = MutableStateFlow(
        CatchFormUiState(
            spotId = savedStateHandle.get<String?>("spotId")
        )
    )
    val state: StateFlow<CatchFormUiState> = _state.asStateFlow()

    init {
        // 加载所有钓点列表（供表单内选择）
        loadAllSpots()

        val spotId = _state.value.spotId
        if (!spotId.isNullOrBlank()) {
            viewModelScope.launch {
                try {
                    val spot = fishingSpotRepository.getSpotById(spotId)
                    if (spot != null) {
                        _state.update {
                            it.copy(
                                spot = spot,
                                river = spot.river.orEmpty(),
                                city = spot.city.orEmpty(),
                                locationDetail = spot.locationDetail.orEmpty()
                            )
                        }
                    } else {
                        Log.w(TAG, "init: spotId=$spotId 在本地数据库中未找到")
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "init: 加载钓点失败", e)
                }
            }
        }
    }

    private fun loadAllSpots() {
        viewModelScope.launch {
            try {
                fishingSpotRepository.getAllSpots().collect { spots ->
                    _state.update { it.copy(allSpots = spots) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "loadAllSpots: 加载钓点列表失败", e)
            }
        }
    }

    fun openSpotPicker() {
        _state.update { it.copy(showSpotPicker = true) }
    }

    fun closeSpotPicker() {
        _state.update { it.copy(showSpotPicker = false) }
    }

    fun selectSpot(spot: FishingSpot) {
        _state.update {
            it.copy(
                spotId = spot.id,
                spot = spot,
                river = spot.river.orEmpty(),
                city = spot.city.orEmpty(),
                locationDetail = spot.locationDetail.orEmpty(),
                showSpotPicker = false
            )
        }
    }

    fun update(transform: (CatchFormUiState) -> CatchFormUiState) {
        _state.update(transform)
    }

    fun addPhotoUris(uris: List<String>) {
        _state.update { s ->
            val merged = (s.photoUris + uris).distinct().take(4)
            s.copy(photoUris = merged)
        }
    }

    fun removePhoto(uri: String) {
        _state.update { it.copy(photoUris = it.photoUris.filterNot { u -> u == uri }) }
    }

    fun parseVoiceInput(text: String) {
        // 正则解析逻辑
        // 种类: "一条翘嘴", "钓到了鳜鱼"
        val speciesPattern = Pattern.compile("(一条|钓到了|捕获了|品种是)?([\\u4e00-\\u9fa5]{2,6})(了|，|。|$)")
        val speciesMatcher = speciesPattern.matcher(text)
        var parsedSpecies = ""
        if (speciesMatcher.find()) {
            parsedSpecies = speciesMatcher.group(2) ?: ""
        }

        // 长度: "50厘米", "50cm"
        val lengthPattern = Pattern.compile("(\\d+)(厘米|cm)")
        val lengthMatcher = lengthPattern.matcher(text)
        var parsedLength = ""
        if (lengthMatcher.find()) {
            parsedLength = lengthMatcher.group(1) ?: ""
        }

        // 重量: "3公斤", "3kg", "3000克"
        val weightPattern = Pattern.compile("(\\d+)(公斤|kg|克|g)")
        val weightMatcher = weightPattern.matcher(text)
        var parsedWeight = ""
        if (weightMatcher.find()) {
            val value = weightMatcher.group(1)?.toFloatOrNull() ?: 0f
            val unit = weightMatcher.group(2) ?: ""
            parsedWeight = if (unit == "公斤" || unit == "kg") (value * 1000).toInt().toString() else value.toInt().toString()
        }

        // 钓点: "在梓江钓的", "位置是梓潼"
        val spotPattern = Pattern.compile("(在|位置是|于)([^钓]{2,8})(钓的|。|，|$)")
        val spotMatcher = spotPattern.matcher(text)
        var parsedRiver = ""
        if (spotMatcher.find()) {
            parsedRiver = spotMatcher.group(2) ?: ""
        }

        _state.update { it.copy(
            species = if (parsedSpecies.isNotBlank()) parsedSpecies else it.species,
            lengthCm = if (parsedLength.isNotBlank()) parsedLength else it.lengthCm,
            weightG = if (parsedWeight.isNotBlank()) parsedWeight else it.weightG,
            river = if (parsedRiver.isNotBlank()) parsedRiver else it.river
        ) }
    }

    fun save() {
        val s = _state.value
        if (s.species.isBlank()) {
            _state.update { it.copy(errorMessage = "请选择/填写鱼种") }
            return
        }
        val spotId = s.spotId
        if (spotId.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = "未关联钓点，请重新选择钓点") }
            return
        }

        viewModelScope.launch {
            try {
                _state.update { it.copy(isSaving = true, errorMessage = null, saved = false) }
                Log.d(TAG, "开始保存: spotId=$spotId, species=${s.species}")

                // 1. 尝试查询本地钓点（不再强制阻断，FK已移除）
                val spot = try {
                    fishingSpotRepository.getSpotById(spotId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "查询钓点失败，继续保存", e)
                    null
                }
                if (spot != null) {
                    Log.d(TAG, "找到本地钓点: ${spot.name}")
                } else {
                    Log.w(TAG, "spotId=$spotId 在本地数据库中未找到，仍继续保存")
                }

                // 2. 可选：获取天气数据（失败不阻塞保存）
                val resolved = try {
                    when {
                        spot?.qWeatherLocationId?.isNotBlank() == true -> com.lurecalendar.app.domain.model.ResolvedLocation(
                            id = spot.qWeatherLocationId!!,
                            name = spot.city ?: "",
                            adm1 = null,
                            adm2 = spot.city,
                            lat = null,
                            lon = null
                        )
                        spot?.city?.isNotBlank() == true -> weatherRepository.resolveLocation(spot.city!!).getOrNull()
                        s.city.isNotBlank() -> weatherRepository.resolveLocation(s.city).getOrNull()
                        else -> null
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "天气位置解析失败，跳过天气", e)
                    null
                }

                val weather = try {
                    resolved?.id?.let { id ->
                        weatherRepository.getWeather(
                            location = id,
                            latitude = resolved.lat,
                            longitude = resolved.lon,
                            waterType = spot?.waterType ?: "淡水",
                            forceRefresh = true
                        ).getOrNull()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "获取天气失败，跳过天气", e)
                    null
                }

                // 3. 构建 CatchRecord
                val record = CatchRecord(
                    id = UUID.randomUUID().toString(),
                    spotId = spotId,
                    species = s.species.trim(),
                    length = s.lengthCm.toFloatOrNull(),
                    weight = s.weightG.toFloatOrNull(),
                    photoUris = s.photoUris.filter { it.isNotBlank() },
                    weatherKey = resolved?.id,
                    catchTime = s.catchTime,
                    bait = s.bait.ifBlank { null },
                    rod = s.rod.ifBlank { null },
                    note = s.note.ifBlank { null },
                    released = false,
                    river = s.river.ifBlank { spot?.river },
                    city = s.city.ifBlank { spot?.city },
                    locationDetail = s.locationDetail.ifBlank { spot?.locationDetail },
                    count = s.count.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                    temperature = weather?.current?.temperature,
                    humidity = weather?.current?.humidity,
                    pressure = weather?.current?.pressure,
                    fishingIndex = null,
                    lureType = s.lureType.ifBlank { null },
                    rigType = s.rigType.ifBlank { null },
                    structureZone = s.structureZone.ifBlank { null },
                    waterClarity = s.waterClarity.ifBlank { null },
                    windShoreRelation = s.windShoreRelation.ifBlank { null }
                )
                Log.d(TAG, "CatchRecord 构建完成: id=${record.id}")

                // 4. 写入数据库
                catchRecordRepository.saveCatch(record)
                Log.d(TAG, "保存成功")
                _state.update { it.copy(isSaving = false, saved = true) }

            } catch (e: CancellationException) {
                Log.d(TAG, "保存被取消")
                throw e   // 必须重新抛出，不能吞掉 CancellationException
            } catch (e: Exception) {
                Log.e(TAG, "保存流程异常", e)
                _state.update { it.copy(isSaving = false, errorMessage = "保存失败: ${e.localizedMessage ?: "未知错误"}") }
            }
        }
    }

    companion object {
        private const val TAG = "CatchFormVM"
    }
}
