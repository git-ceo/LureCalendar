package com.lurecalendar.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getIsLoggedIn(): Flow<Boolean>
    fun isOnboardingCompleted(): Flow<Boolean>
    suspend fun setOnboardingCompleted(completed: Boolean)
    fun getUsername(): Flow<String>
    fun getPhone(): Flow<String>
    suspend fun login(phone: String, password: String): Result<String>
    suspend fun register(phone: String, password: String, username: String): Result<String>
    suspend fun logout()
}
