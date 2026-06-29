package com.lurecalendar.app.data.remote.api

import com.lurecalendar.app.domain.model.CatchRecord
import com.lurecalendar.app.domain.model.FishingSpot
import com.squareup.moshi.Json
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PUT

/**
 * 远程服务器 API 接口
 * 对应公网服务器上的 lurecalendar-api
 */
interface LureCalendarApiService {

    // --- 用户认证 ---
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    // --- 钓点同步 ---
    @GET("api/spots")
    suspend fun getAllSpots(): List<FishingSpot>

    @GET("api/spots/leaderboard")
    suspend fun getSpotLeaderboard(@retrofit2.http.Query("spot_id") spotId: String): List<SpotLeaderboardEntry>

    @POST("api/spots/sync")
    suspend fun syncSpots(@Body spots: List<FishingSpot>): Response<SyncResponse>

    @PUT("api/spots")
    suspend fun updateSpot(@Body spot: FishingSpot): Response<Unit>

    // --- 鱼获同步 ---
    @GET("api/catches")
    suspend fun getAllCatches(): List<CatchRecord>

    @POST("api/catches/sync")
    suspend fun syncCatches(@Body catches: List<CatchRecord>): Response<SyncResponse>

    // --- 天气与水位缓存 ---
    @GET("api/weather/cache")
    suspend fun getWeatherCache(@retrofit2.http.Query("location_key") locationKey: String): Response<WeatherCacheResponse>

    @POST("api/weather/cache")
    suspend fun saveWeatherCache(@Body request: WeatherCacheRequest): Response<SyncResponse>

    @GET("api/water-level/cache")
    suspend fun getWaterLevelCache(@retrofit2.http.Query("station_id") stationId: String): Response<WaterLevelCacheResponse>

    @POST("api/water-level/cache")
    suspend fun saveWaterLevelCache(@Body request: WaterLevelCacheRequest): Response<SyncResponse>

    // --- 用户个人信息 ---
    @GET("api/user/profile")
    suspend fun getProfile(@retrofit2.http.Query("phone") phone: String): Response<UserProfileResponse>

    @POST("api/user/update")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<BasicResponse>

    @POST("api/user/location")
    suspend fun updateLocation(@Body request: UpdateLocationRequest): Response<SyncResponse>

    @GET("api/users/locations")
    suspend fun getAnglerLocations(@retrofit2.http.Query("viewer_phone") viewerPhone: String): List<AnglerLocationResponse>

    @GET("api/user/achievements")
    suspend fun getAchievements(@retrofit2.http.Query("phone") phone: String): List<AchievementResponse>

    @GET("api/videos")
    suspend fun getTechniqueVideos(): List<VideoResponse>

    // --- 钓友圈 (朋友圈) ---
    @GET("api/moments")
    suspend fun getMoments(
        @retrofit2.http.Query("type") type: String = "square",
        @retrofit2.http.Query("phone") phone: String? = null,
        @retrofit2.http.Query("page") page: Int = 0,
        @retrofit2.http.Query("size") size: Int = 20
    ): List<MomentResponse>

    @POST("api/moments")
    suspend fun createMoment(@Body request: CreateMomentRequest): MomentResponse

    @POST("api/moments/{id}/like")
    suspend fun toggleLike(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Query("phone") phone: String
    ): Response<LikeResponse>

    @GET("api/moments/{id}/comments")
    suspend fun getComments(@retrofit2.http.Path("id") id: String): List<CommentResponse>

    @POST("api/moments/{id}/comment")
    suspend fun createComment(
        @retrofit2.http.Path("id") id: String,
        @Body request: CreateCommentRequest
    ): Response<BasicResponse>

    // --- 用户搜索与好友 ---
    @GET("api/users/search")
    suspend fun searchUsers(
        @retrofit2.http.Query("keyword") keyword: String,
        @retrofit2.http.Query("phone") phone: String
    ): List<UserSearchResponse>

    @POST("api/follow/{target_phone}")
    suspend fun followUser(
        @retrofit2.http.Path("target_phone") targetPhone: String,
        @retrofit2.http.Query("phone") phone: String
    ): Response<Map<String, String>>

    @GET("api/friend-requests")
    suspend fun getFriendRequests(@retrofit2.http.Query("phone") phone: String): List<FriendRequestResponse>

    @PUT("api/friend-requests/{id}")
    suspend fun handleFriendRequest(
        @retrofit2.http.Path("id") id: Int,
        @retrofit2.http.Query("action") action: String
    ): Response<SyncResponse>

    @POST("api/users/radar/start")
    suspend fun startRadar(@retrofit2.http.Query("phone") phone: String): Response<RadarResponse>

    @POST("api/users/radar/add")
    suspend fun addRadar(
        @retrofit2.http.Query("phone") phone: String,
        @Body request: Map<String, String>
    ): Response<SyncResponse>

    // --- 文件上传 ---
    @Multipart
    @POST("api/upload")
    suspend fun uploadImage(@Part file: MultipartBody.Part): Response<UploadResponse>

    // --- 钓鱼指数 ---
    @POST("api/weather/index")
    suspend fun getFishingIndex(@Body request: FishingIndexRequest): Response<Map<String, Any>>

    // --- 鱼获统计 ---
    @GET("api/catches/stats")
    suspend fun getCatchStats(@retrofit2.http.Query("phone") phone: String): Response<CatchStatsResponse>

    @GET("api/catches/report")
    suspend fun generateCatchReport(@retrofit2.http.Query("phone") phone: String): Response<UploadResponse>

    // --- 装备统计 ---
    @GET("api/gear/stats")
    suspend fun getGearStats(
        @retrofit2.http.Query("phone") phone: String,
        @retrofit2.http.Query("rod_name") rodName: String
    ): Response<GearStatsResponse>

    // --- 内容百科库 (v3.0) ---
    @GET("api/encyclopedia/fish")
    suspend fun listFishEncyclopedia(
        @retrofit2.http.Query("category") category: String? = null,
        @retrofit2.http.Query("q") query: String? = null
    ): List<FishEncyclopediaResponse>

    @GET("api/encyclopedia/fish/{name}")
    suspend fun getFishDetail(
        @retrofit2.http.Path("name") name: String
    ): Response<FishDetailEnvelope>

    @GET("api/encyclopedia/lures")
    suspend fun listLures(
        @retrofit2.http.Query("category") category: String? = null,
        @retrofit2.http.Query("target") target: String? = null,
        @retrofit2.http.Query("swim_layer") swimLayer: String? = null
    ): List<LureResponse>

    @GET("api/encyclopedia/lures/match")
    suspend fun matchLures(
        @retrofit2.http.Query("species") species: String,
        @retrofit2.http.Query("water_temp") waterTemp: Double? = null,
        @retrofit2.http.Query("water_type") waterType: String? = null,
        @retrofit2.http.Query("hour") hour: Int? = null
    ): Response<LureMatchEnvelope>

    @GET("api/encyclopedia/guides")
    suspend fun listGuides(
        @retrofit2.http.Query("category") category: String? = null,
        @retrofit2.http.Query("season") season: String? = null,
        @retrofit2.http.Query("target") target: String? = null,
        @retrofit2.http.Query("page") page: Int = 0,
        @retrofit2.http.Query("size") size: Int = 20
    ): List<FishingGuideResponse>

    @GET("api/encyclopedia/guides/{id}")
    suspend fun getGuideDetail(
        @retrofit2.http.Path("id") id: Int
    ): Response<FishingGuideEnvelope>

    @GET("api/astronomy")
    suspend fun getAstronomy(
        @retrofit2.http.Query("lat") lat: Double,
        @retrofit2.http.Query("lon") lon: Double,
        @retrofit2.http.Query("date") date: String? = null
    ): Response<AstronomyEnvelope>
}

// --- 请求与响应数据结构 ---

data class GearStatsResponse(
    val rod_name: String,
    val total_count: Int,
    val species_dist: List<SpeciesCount>,
    val weight_trend: List<WeightTrendPoint>
)

data class SpeciesCount(val species: String, val count: Int)
data class WeightTrendPoint(val time: String, val weight: Double)

data class CatchStatsResponse(
    val totalCount: Int,
    val totalWeight: Double,
    val maxWeight: Double,
    val maxSpecies: String,
    val fishingDays: Int,
    val monthlyCount: Int
)

data class UploadResponse(
    val success: Boolean,
    val url: String?,
    val message: String?
)

data class UserProfileResponse(
    val success: Boolean,
    val username: String?,
    val signature: String?,
    val avatarUrl: String?,
    val backgroundUrl: String?,
    val createTime: Long?
)

data class AchievementResponse(
    val id: String,
    val name: String,
    val desc: String,
    val unlocked: Boolean,
    val progress: String
)

data class VideoResponse(
    val id: String,
    val title: String,
    val platform: String,
    val videoUrl: String,
    val thumbnail: String,
    val author: String
)

data class UpdateProfileRequest(
    val phone: String,
    val username: String? = null,
    val signature: String? = null,
    val avatarUrl: String? = null,
    val backgroundUrl: String? = null
)

data class MomentResponse(
    val id: String,
    val userId: String,
    val username: String,
    val avatarUrl: String?,
    val content: String,
    val photos: List<String>,
    val visibility: String = "public",
    val createTime: Long,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean = false,
    val isSending: Boolean = false, // 本地 UI 状态
    val isError: Boolean = false    // 本地 UI 状态
)

data class CommentResponse(
    val id: Int,
    val userId: String,
    val username: String,
    val avatarUrl: String?,
    val content: String,
    val createTime: Long,
    val replies: List<CommentResponse> = emptyList()
)

data class CreateMomentRequest(
    val phone: String,
    val content: String,
    val photos: List<String> = emptyList(),
    val visibility: String = "public"
)

data class LikeResponse(
    val liked: Boolean,
    @Json(name = "like_count") val likeCount: Int
)

data class UserSearchResponse(
    val phone: String,
    val username: String,
    val avatarUrl: String?,
    val signature: String?,
    val isFollowing: Boolean,
    val addConfirm: Boolean
)

data class FriendRequestResponse(
    val id: Int,
    val fromPhone: String,
    val username: String,
    val avatarUrl: String?,
    val createTime: Long
)

data class RadarResponse(
    val code: String,
    val expires_in: Int
)

data class WeatherCacheRequest(
    val location_key: String,
    val weather_data: com.lurecalendar.app.domain.model.WeatherData,
    val timestamp: Long
)

data class WeatherCacheResponse(
    val success: Boolean,
    val data: com.lurecalendar.app.domain.model.WeatherData?,
    val timestamp: Long?
)

data class WaterLevelCacheRequest(
    val station_id: String,
    val water_level_data: com.lurecalendar.app.domain.model.WaterLevel,
    val timestamp: Long
)

data class WaterLevelCacheResponse(
    val success: Boolean,
    val data: com.lurecalendar.app.domain.model.WaterLevel?,
    val timestamp: Long?
)

data class LoginRequest(
    val phone: String,
    val password: String
)

data class RegisterRequest(
    val phone: String,
    val password: String,
    val username: String
)

data class AuthResponse(
    val success: Boolean,
    val message: String?,
    val token: String?,
    val userId: String?,
    val username: String?
)

data class SyncResponse(
    val success: Boolean,
    val syncedCount: Int,
    val message: String? = null
)

data class BasicResponse(
    val success: Boolean,
    val message: String? = null
)

data class SpotLeaderboardEntry(
    val username: String,
    val avatarUrl: String?,
    val species: String,
    val weight: Float?,
    val length: Float?,
    val photo: String?,
    val catchTime: Long
)

data class UpdateLocationRequest(
    val phone: String,
    val latitude: Double,
    val longitude: Double
)

data class CreateCommentRequest(
    val phone: String,
    val content: String
)

data class FishingIndexRequest(
    val phone: String? = null,
    val latitude: Double,
    val longitude: Double,
    val species: String? = null
)

data class AnglerLocationResponse(
    val phone: String,
    val username: String,
    val avatarUrl: String?,
    val latitude: Double,
    val longitude: Double,
    val lastActive: Long
)

// === 内容百科库 DTO (v3.0) ===

data class FishEncyclopediaResponse(
    val id: Int,
    val name: String,
    val alias: String?,
    val scientificName: String?,
    val family: String?,
    val category: String?,
    val distribution: String?,
    val habitat: String?,
    val feedingHabit: String?,
    val bodySize: String?,
    val bestSeason: String?,
    val bestHours: String?,
    val optimalTemp: String?,
    val recommendedLures: String?,
    val techniqueTips: String?,
    val imageUrl: String?,
    val description: String?,
    val source: String?,
    val updateTime: Long?
)

data class FishDetailEnvelope(
    val success: Boolean,
    val data: FishEncyclopediaResponse?,
    val message: String? = null
)

data class LureResponse(
    val id: Int,
    val name: String,
    val category: String?,
    val subType: String?,
    val swimLayer: String?,
    val weightRange: String?,
    val lengthRange: String?,
    val divingDepth: String?,
    val targetSpecies: String?,
    val suitableWaterTemp: String?,
    val suitableWaterType: String?,
    val technique: String?,
    val colorTip: String?,
    val pros: String?,
    val cons: String?,
    val icon: String?,
    val imageUrl: String?,
    val description: String?,
    val source: String?,
    val updateTime: Long?,
    val matchScore: Int? = null
)

data class LureMatchEnvelope(
    val success: Boolean,
    val species: String?,
    val recommendations: List<LureResponse>?,
    val message: String? = null
)

data class FishingGuideResponse(
    val id: Int,
    val title: String,
    val category: String?,
    val targetSpecies: String?,
    val season: String?,
    val waterType: String?,
    val summary: String?,
    val content: String?,
    val coverUrl: String?,
    val tags: String?,
    val source: String?,
    val sourceUrl: String?,
    val viewCount: Int? = 0,
    val createTime: Long?,
    val updateTime: Long?
)

data class FishingGuideEnvelope(
    val success: Boolean,
    val data: FishingGuideResponse?,
    val message: String? = null
)

data class AstronomyData(
    val date: String?,
    val sunrise: String?,
    val sunset: String?,
    val moonrise: String?,
    val moonset: String?,
    @Json(name = "moon_phase") val moonPhase: String?,
    @Json(name = "moon_illumination") val moonIllumination: Float?,
    @Json(name = "lunar_date") val lunarDate: String?,
    @Json(name = "is_closed_season") val isClosedSeason: Int?,
    @Json(name = "closed_season_note") val closedSeasonNote: String?
)

data class AstronomyEnvelope(
    val success: Boolean,
    val data: AstronomyData?,
    val message: String? = null
)
