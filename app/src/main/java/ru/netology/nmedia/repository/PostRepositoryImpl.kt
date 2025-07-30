package ru.netology.nmedia.repository

import androidx.lifecycle.map
import okhttp3.internal.concurrent.TaskRunner.Companion.logger
import okio.IOException
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError


class PostRepositoryImpl(private val dao: PostDao) : PostRepository {
    override val data = dao.getAll().map { posts ->
        posts.map { it.toDto() }
    }

    override suspend fun getAll() {
        try {
            val response = PostsApi.service.getAll()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(body.toEntity())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun save(post: Post) {
        try {
            val response = PostsApi.service.save(post)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(PostEntity.fromDto(body))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun removeById(id: Long) {
        try {
            dao.removeById(id)
            val response = PostsApi.service.removeById(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }


    override suspend fun likeById(id: Long) {
        var originalPost: PostEntity? = null

        try {
            // 1. Сохраняем оригинальное состояние
            originalPost = dao.getById(id) ?: return

            // 2. Оптимистичное обновление
            val updatedPost = originalPost.copy(
                likedByMe = !originalPost.likedByMe,
                likes = if (originalPost.likedByMe) originalPost.likes - 1 else originalPost.likes + 1
            )
            dao.insert(updatedPost)

            // 3. Отправка на сервер
            val response = if (updatedPost.likedByMe) {
                PostsApi.service.likeById(id)
            } else {
                PostsApi.service.dislikeById(id)
            }

            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
        } catch (e: Exception) {
            // 4. Откат изменений при ошибке
            originalPost?.let {
                try {
                    dao.insert(it)
                } catch (e: Exception) {
                    logger.severe("Failed to revert like changes: ${e.message}")
                }
            }

            when (e) {
                is IOException -> throw NetworkError
                is ApiError -> throw e
                else -> throw UnknownError
            }
        }
    }
}