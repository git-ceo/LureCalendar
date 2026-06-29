package com.lurecalendar.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lurecalendar.app.data.remote.api.LureCalendarApiService
import com.lurecalendar.app.data.remote.api.LoginRequest
import com.lurecalendar.app.data.remote.api.RegisterRequest
import com.lurecalendar.app.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: LureCalendarApiService
) : AuthRepository {

    private val KEY_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    private val KEY_USERNAME = stringPreferencesKey("username")
    private val KEY_PHONE = stringPreferencesKey("phone")
    private val KEY_TOKEN = stringPreferencesKey("token")

    override fun getIsLoggedIn(): Flow<Boolean> = context.dataStore.data.map { it[KEY_LOGGED_IN] ?: false }
    override fun isOnboardingCompleted(): Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_COMPLETED] ?: false }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }
    override fun getUsername(): Flow<String> = context.dataStore.data.map { it[KEY_USERNAME] ?: "" }
    override fun getPhone(): Flow<String> = context.dataStore.data.map { it[KEY_PHONE] ?: "" }

    override suspend fun login(phone: String, password: String): Result<String> {
        return runCatching {
            val resp = apiService.login(LoginRequest(phone, password))
            val body = resp.body()
            if (resp.isSuccessful && body?.success == true) {
                context.dataStore.edit { prefs ->
                    prefs[KEY_LOGGED_IN] = true
                    prefs[KEY_USERNAME] = body.username ?: ""
                    prefs[KEY_PHONE] = phone
                    prefs[KEY_TOKEN] = body.token ?: ""
                }
                body.message ?: "登录成功"
            } else {
                throw Exception(body?.message ?: "登录失败")
            }
        }
    }

    override suspend fun register(phone: String, password: String, username: String): Result<String> {
        return runCatching {
            val resp = apiService.register(RegisterRequest(phone, password, username))
            val body = resp.body()
            if (resp.isSuccessful && body?.success == true) {
                context.dataStore.edit { prefs ->
                    prefs[KEY_LOGGED_IN] = true
                    prefs[KEY_USERNAME] = body.username ?: ""
                    prefs[KEY_PHONE] = phone
                    prefs[KEY_TOKEN] = body.token ?: ""
                }
                body.message ?: "注册成功"
            } else {
                throw Exception(body?.message ?: "注册失败")
            }
        }
    }

    override suspend fun logout() {
        context.dataStore.edit { it.clear() }
    }
}
