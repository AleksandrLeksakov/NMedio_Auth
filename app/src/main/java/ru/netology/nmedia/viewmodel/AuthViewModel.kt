package ru.netology.nmedia.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.Response
import ru.netology.nmedia.api.AuthApi  // Используем AuthApi вместо PostsApi
import ru.netology.nmedia.dto.AuthResponse
import ru.netology.nmedia.auth.AppAuth

class AuthViewModel : ViewModel() {
    val isAuthorized: LiveData<Boolean> = AppAuth.getInstance()
        .state
        .map { it != null }
        .asLiveData(Dispatchers.Default)

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> get() = _authState

    fun authenticate(login: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val response = AuthApi.service.authenticate(login, password)
                handleAuthResponse(response)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Network error")
            }
        }
    }

    private fun handleAuthResponse(response: Response<AuthResponse>) {
        if (response.isSuccessful) {
            val authResponse = response.body()
            authResponse?.let {
                AppAuth.getInstance().saveAuth(it.id, it.token)
                _authState.value = AuthState.Success
            } ?: run {
                _authState.value = AuthState.Error("Invalid response")
            }
        } else {
            _authState.value = when (response.code()) {
                400 -> AuthState.Error("Invalid login or password")
                404 -> AuthState.Error("User not found")
                else -> AuthState.Error("Authentication failed: ${response.code()}")
            }
        }
    }

    fun logout() {
        AppAuth.getInstance().removeAuth()
        _authState.value = AuthState.Idle
    }
}



