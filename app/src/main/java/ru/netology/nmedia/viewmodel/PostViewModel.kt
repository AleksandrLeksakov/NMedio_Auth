package ru.netology.nmedia.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.File
import java.io.IOException

//private val PostViewModel.it: Post
private val empty = Post(
    id = 0,
    authorId = 0L,
    content = "",
    author = "",
    authorAvatar = "",
    likedByMe = false,
    likes = 0,
    published = "",
    isHidden = false,
    attachment = null

)

class PostViewModel(application: Application) : AndroidViewModel(application) {
    // Репозиторий для работы с данными
    private val repository: PostRepository =
        PostRepositoryImpl(AppDb.getInstance(context = application).postDao())

    // Основные данные постов (фильтруем скрытые)
    val data: LiveData<List<Post>> = AppAuth.getInstance().state.flatMapLatest { token ->
repository.data
    .map { posts -> posts.map { it.copy(ownedByMe = it.authorId == token?.id) } }
    }
        .map { posts -> posts.filter { !it.isHidden } }
        .asLiveData(Dispatchers.Default)

    // сохранить фото
    private val _photo = MutableLiveData<PhotoModel?>(null)
val photo: LiveData<PhotoModel?>
    get() = _photo

    // Количество скрытых постов
    val hiddenCount: LiveData<Int> = repository.hiddenCount
        .asLiveData()

    // Событие прокрутки к началу списка
    private val _scrollToTopEvent = SingleLiveEvent<Unit>()
    val scrollToTopEvent: LiveData<Unit> = _scrollToTopEvent

    // Состояние загрузки/обновления
    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    // Событие для показа новых постов
    private val _showNewPostsEvent = SingleLiveEvent<Unit>()
    val showNewPostsEvent: LiveData<Unit> = _showNewPostsEvent

    // Редактируемый пост
    private val edited = MutableLiveData(empty)

    // Событие успешного создания поста
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    init {
        loadPosts()
        loadNewPosts()
    }

    fun savePhoto(uri: Uri, file: File) {
        _photo.value = PhotoModel(uri, file)
    }

    fun removePhoto() {
        _photo.value = null
    }

    // Загрузка новых постов (свежих чем текущие)
    private fun loadNewPosts() {
        viewModelScope.launch {
            repository.getNewer(repository.getLatestId() ?: 0L).collect()
        }
    }

    // Загрузка всех постов
    fun loadPosts() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(loading = true)
            repository.getAll()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    // Показать все скрытые посты
    fun showAllHiddenPosts() {
        viewModelScope.launch {
            try {
                repository.showAllHiddenPosts()
                _scrollToTopEvent.postValue(Unit) // Триггерим событие прокрутки вверх
            } catch (e: Exception) {
                _dataState.value = FeedModelState(error = true)
            }
        }
    }

    // Обновление данных (pull-to-refresh)
    fun refreshPosts() = viewModelScope.launch {
        try {
            _dataState.value = FeedModelState(refreshing = true)
            repository.getAll()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    // Сохранение поста (создание/редактирование)
    fun save() {
        edited.value?.let { post ->
            _postCreated.value = Unit
            viewModelScope.launch {
                try {
                    val photoFile = _photo.value?.file
                    repository.save(post, photoFile)
                    _photo.value = null // очищаем фото после сохранения
                    _dataState.value = FeedModelState()
                } catch (e: Exception) {
                    _dataState.value = FeedModelState(error = true)
                }
            }
        }
        edited.value = empty // Сбрасываем редактируемый пост
    }

    // Начать редактирование поста
    fun edit(post: Post) {
        edited.value = post
    }

    // Изменение содержимого поста при редактировании
    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) return
        edited.value = edited.value?.copy(content = text)
    }

    // Лайк поста
    fun likeById(id: Long) = viewModelScope.launch {
        try {
            repository.likeById(id)
        } catch (e: IOException) {
            _dataState.value = FeedModelState(error = true, networkError = true)
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }

    // Удаление поста
    fun removeById(id: Long) = viewModelScope.launch {
        try {
            repository.removeById(id)
        } catch (e: IOException) {
            _dataState.value = FeedModelState(error = true, networkError = true)
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        }
    }
}