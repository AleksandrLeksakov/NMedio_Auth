package ru.netology.nmedia.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import ru.netology.nmedia.auth.AppAuth

class AuthViewModel : ViewModel() {
    val isAuthorized: LiveData<Boolean> = AppAuth.getInstance()
        .state
        .map { it != null }
        .asLiveData(Dispatchers.Default)
}