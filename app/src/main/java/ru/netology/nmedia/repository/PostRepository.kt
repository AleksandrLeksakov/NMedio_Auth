package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.Post
import java.io.File

interface PostRepository {
    /**
     * Поток (Flow) со списком видимых постов.
     * Автоматически обновляется при изменениях в базе данных.
     */
    val data: Flow<List<Post>>

    /**
     * Поток (Flow) с количеством скрытых (новых) постов.
     * Используется для отображения уведомлений о новых постах.
     */
    val hiddenCount: Flow<Int>

    /**
     * Загружает все посты из сети и сохраняет их в базу данных.
     * @throws NetworkError при проблемах с сетью
     * @throws ApiError при ошибках API
     * @throws UnknownError при других ошибках
     */
    suspend fun getAll()

    /**
     * Сохраняет пост (создание или обновление).
     * @param post Пост для сохранения
     * @throws NetworkError при проблемах с сетью
     * @throws ApiError при ошибках API
     * @throws UnknownError при других ошибках
     */
    suspend fun save(post: Post, photo: File?)

    /**
     * Удаляет пост по идентификатору.
     * @param id Идентификатор поста для удаления
     * @throws NetworkError при проблемах с сетью
     * @throws ApiError при ошибках API
     * @throws UnknownError при других ошибках
     */
    suspend fun removeById(id: Long)

    /**
     * Переключает состояние "лайка" у поста.
     * @param id Идентификатор поста
     * @throws NetworkError при проблемах с сетью
     * @throws ApiError при ошибках API
     * @throws UnknownError при других ошибках
     */
    suspend fun likeById(id: Long)

    /**
     * Получает новые посты, начиная с указанного идентификатора.
     * @param id Идентификатор, начиная с которого искать новые посты
     * @return Flow с количеством новых постов
     */
    fun getNewer(id: Long): Flow<Int>

    /**
     * Делает все скрытые посты видимыми.
     * Используется при нажатии на уведомление о новых постах.
     */
    suspend fun showAllHiddenPosts()

    /**
     * Возвращает идентификатор самого свежего поста в базе.
     * @return Идентификатор последнего поста или null если база пуста
     */
    suspend fun getLatestId(): Long?
}