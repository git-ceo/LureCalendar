package com.lurecalendar.app.ui.screens.social

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lurecalendar.app.data.remote.api.CommentResponse
import com.lurecalendar.app.data.remote.api.CreateCommentRequest
import com.lurecalendar.app.data.remote.api.CreateMomentRequest
import com.lurecalendar.app.data.remote.api.LureCalendarApiService
import com.lurecalendar.app.data.remote.api.MomentResponse
import com.lurecalendar.app.data.remote.api.UserProfileResponse
import com.lurecalendar.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class SocialViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val apiService: LureCalendarApiService
) : ViewModel() {

    var moments = mutableStateListOf<MomentResponse>()
    var isRefreshing by mutableStateOf(false)
    var feedError by mutableStateOf<String?>(null)
    var showPostDialog by mutableStateOf(false)
    var newPostContent by mutableStateOf("")
    var newPostVisibility by mutableStateOf("public")
    var selectedTab by mutableStateOf("square") // square or friends
    var currentUserProfile by mutableStateOf<UserProfileResponse?>(null)

    var commentSheetMoment by mutableStateOf<MomentResponse?>(null)
    var comments = mutableStateListOf<CommentResponse>()
    var isLoadingComments by mutableStateOf(false)
    var newCommentContent by mutableStateOf("")
    var commentError by mutableStateOf<String?>(null)
    var serverSupportsComments by mutableStateOf(true)
    var serverSupportsLikes by mutableStateOf(true)

    val commentPreviews = mutableStateMapOf<String, List<CommentResponse>>()
    private val commentPreviewLoading = mutableStateMapOf<String, Boolean>()

    private fun ResponseBody?.previewText(): String? {
        if (this == null) return null
        return runCatching { string() }.getOrNull()?.trim()?.take(200)?.ifBlank { null }
    }

    private fun Response<*>.errorPreview(): String? = errorBody().previewText()

    init {
        refresh()
        loadCurrentUserProfile()
    }

    private fun loadCurrentUserProfile() {
        viewModelScope.launch {
            val phone = authRepository.getPhone().first()
            if (phone.isNotBlank()) {
                runCatching { apiService.getProfile(phone) }.onSuccess { resp ->
                    if (resp.isSuccessful) {
                        currentUserProfile = resp.body()
                    }
                }
            }
        }
    }

    fun switchTab(tab: String) {
        selectedTab = tab
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing = true
            feedError = null
            serverSupportsComments = true
            serverSupportsLikes = true
            runCatching {
                val phone = authRepository.getPhone().first()
                if (phone.isBlank()) error("未登录")
                apiService.getMoments(type = selectedTab, phone = phone, page = 0, size = 50)
            }.onSuccess { list ->
                moments.clear()
                moments.addAll(list)
            }.onFailure { e ->
                feedError = e.message ?: "内容加载失败"
            }
            isRefreshing = false
        }
    }

    fun postMoment() {
        val content = newPostContent
        val visibility = newPostVisibility
        if (content.isBlank()) return
        showPostDialog = false
        newPostContent = ""

        viewModelScope.launch {
            val phone = authRepository.getPhone().first()
            val username = authRepository.getUsername().first()
            if (phone.isBlank()) return@launch
            
            // 乐观更新：添加一个伪动态
            val tempId = "temp_${System.currentTimeMillis()}"
            val tempPost = MomentResponse(
                id = tempId,
                userId = phone,
                username = username,
                avatarUrl = null,
                content = content,
                photos = emptyList(),
                visibility = visibility,
                createTime = System.currentTimeMillis(),
                likeCount = 0,
                commentCount = 0,
                isLiked = false,
                isSending = true
            )
            moments.add(0, tempPost)

            runCatching {
                apiService.createMoment(CreateMomentRequest(phone, content, visibility = visibility))
            }.onSuccess { realPost ->
                // 用真实数据替换伪数据
                val index = moments.indexOfFirst { it.id == tempId }
                if (index != -1) {
                    moments[index] = realPost
                }
            }.onFailure {
                // 标记失败
                val index = moments.indexOfFirst { it.id == tempId }
                if (index != -1) {
                    moments[index] = moments[index].copy(isSending = false, isError = true)
                }
            }
        }
    }

    fun toggleLike(moment: MomentResponse) {
        viewModelScope.launch {
            val phone = authRepository.getPhone().first()
            if (phone.isBlank()) {
                feedError = "请先登录"
                return@launch
            }
            val index = moments.indexOfFirst { it.id == moment.id }
            if (index == -1) return@launch
            
            // 立即切换 UI 状态 (乐观)
            val oldPost = moments[index]
            val newIsLiked = !oldPost.isLiked
            val newLikeCount = if (newIsLiked) oldPost.likeCount + 1 else oldPost.likeCount - 1
            moments[index] = oldPost.copy(isLiked = newIsLiked, likeCount = newLikeCount.coerceAtLeast(0))
            
            runCatching {
                apiService.toggleLike(moment.id, phone)
            }.onSuccess { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body != null) {
                        moments[index] = moments[index].copy(isLiked = body.liked, likeCount = body.likeCount.coerceAtLeast(0))
                    }
                    return@onSuccess
                }
                if (resp.code() == 404) {
                    serverSupportsLikes = false
                    val extra = resp.errorPreview()
                    feedError = if (extra.isNullOrBlank()) "点赞失败：接口 404（后端可能没部署新版本）" else "点赞失败：接口 404（$extra）"
                } else {
                    val extra = resp.errorPreview()
                    feedError = if (extra.isNullOrBlank()) "点赞失败(HTTP ${resp.code()})" else "点赞失败(HTTP ${resp.code()}：$extra)"
                }
                moments[index] = oldPost
            }.onFailure { e ->
                if (e is HttpException && e.code() == 404) {
                    serverSupportsLikes = false
                    val extra = e.response()?.errorBody().previewText()
                    feedError = if (extra.isNullOrBlank()) "点赞失败：接口 404（后端可能没部署新版本）" else "点赞失败：接口 404（$extra）"
                } else {
                    val extra = (e as? HttpException)?.response()?.errorBody().previewText()
                    feedError = if (extra.isNullOrBlank()) (e.message ?: "点赞失败") else "${e.message ?: "点赞失败"}（$extra）"
                }
                moments[index] = oldPost
            }
        }
    }

    fun openComments(moment: MomentResponse) {
        commentSheetMoment = moment
        newCommentContent = ""
        commentError = null
        loadComments(moment.id)
    }

    fun closeComments() {
        commentSheetMoment = null
        comments.clear()
        newCommentContent = ""
        commentError = null
    }

    fun ensureCommentPreview(momentId: String) {
        if (commentPreviews.containsKey(momentId)) return
        if (commentPreviewLoading[momentId] == true) return
        commentPreviewLoading[momentId] = true
        viewModelScope.launch {
            runCatching { apiService.getComments(momentId) }
                .onSuccess { list ->
                    commentPreviews[momentId] = list.take(2)
                }
            commentPreviewLoading[momentId] = false
        }
    }

    fun loadComments(momentId: String) {
        viewModelScope.launch {
            isLoadingComments = true
            commentError = null
            runCatching {
                apiService.getComments(momentId)
            }.onSuccess { list ->
                comments.clear()
                comments.addAll(list)
                commentPreviews[momentId] = list.take(2)
            }.onFailure { e ->
                if (e is HttpException && e.code() == 404) {
                    serverSupportsComments = false
                    val extra = e.response()?.errorBody().previewText()
                    commentError = if (extra.isNullOrBlank()) "评论加载失败：接口 404（后端可能没部署新版本）" else "评论加载失败：接口 404（$extra）"
                } else {
                    val extra = (e as? HttpException)?.response()?.errorBody().previewText()
                    commentError = if (extra.isNullOrBlank()) (e.message ?: "评论加载失败") else "${e.message ?: "评论加载失败"}（$extra）"
                }
            }
            isLoadingComments = false
        }
    }

    fun postComment() {
        val moment = commentSheetMoment ?: return
        val content = newCommentContent.trim()
        if (content.isBlank()) return
        newCommentContent = ""
        commentError = null

        viewModelScope.launch {
            runCatching {
                val phone = authRepository.getPhone().first()
                if (phone.isBlank()) error("请先登录")
                apiService.createComment(
                    id = moment.id,
                    request = CreateCommentRequest(phone = phone, content = content)
                )
            }.onSuccess { resp ->
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val index = moments.indexOfFirst { it.id == moment.id }
                    if (index != -1) {
                        val old = moments[index]
                        moments[index] = old.copy(commentCount = old.commentCount + 1)
                        commentSheetMoment = moments[index]
                    }
                    loadComments(moment.id)
                } else {
                    if (resp.code() == 404) {
                        serverSupportsComments = false
                        val extra = resp.errorPreview()
                        commentError = if (extra.isNullOrBlank()) "评论发送失败：接口 404（后端可能没部署新版本）" else "评论发送失败：接口 404（$extra）"
                    } else {
                        val extra = resp.errorPreview()
                        val msg = resp.body()?.message
                        commentError = when {
                            !msg.isNullOrBlank() -> msg
                            extra.isNullOrBlank() -> "评论发送失败(HTTP ${resp.code()})"
                            else -> "评论发送失败(HTTP ${resp.code()}：$extra)"
                        }
                    }
                }
            }.onFailure { e ->
                if (e is HttpException && e.code() == 404) {
                    serverSupportsComments = false
                    val extra = e.response()?.errorBody().previewText()
                    commentError = if (extra.isNullOrBlank()) "评论发送失败：接口 404（后端可能没部署新版本）" else "评论发送失败：接口 404（$extra）"
                } else {
                    val extra = (e as? HttpException)?.response()?.errorBody().previewText()
                    commentError = if (extra.isNullOrBlank()) (e.message ?: "评论发送失败") else "${e.message ?: "评论发送失败"}（$extra）"
                }
            }
        }
    }
}
