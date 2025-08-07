package ru.netology.nmedia.repository

import androidx.lifecycle.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import okhttp3.internal.concurrent.TaskRunner.Companion.logger
import okio.IOException
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toDto
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.AppError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError

class PostRepositoryImpl(private val dao: PostDao) : PostRepository {
    override val data = dao.getAllVisible().map { it.toDto() }
    override val hiddenCount = dao.getHiddenCount()
    override suspend fun showAllHiddenPosts() {
        dao.showAllHiddenPosts()

    }


    override suspend fun getAll() {
        try {
            val response = PostsApi.service.getAll()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(body.map { it.toEntity().copy(isHidden = false) })
        } catch (e: IOException) {
            logger.severe("Network error while fetching posts: ${e.message}")
            throw NetworkError
        } catch (e: Exception) {
            logger.severe("Unknown error while fetching posts: ${e.message}")
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
            dao.insert(PostEntity.fromDto(body).copy(isHidden = false))
        } catch (e: IOException) {
            logger.severe("Network error while saving post: ${e.message}")
            throw NetworkError
        } catch (e: Exception) {
            logger.severe("Unknown error while saving post: ${e.message}")
            throw UnknownError
        }
    }

    override suspend fun removeById(id: Long) {
        try {
            // Оптимистичное удаление
            dao.removeById(id)
            val response = PostsApi.service.removeById(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
        } catch (e: IOException) {
            logger.severe("Network error while removing post: ${e.message}")
            throw NetworkError
        } catch (e: Exception) {
            logger.severe("Unknown error while removing post: ${e.message}")
            throw UnknownError
        }
    }

    override suspend fun likeById(id: Long) {
        var originalPost: PostEntity? = null

        try {
            originalPost = dao.getById(id) ?: return

            // Оптимистичное обновление
            val updatedPost = originalPost.copy(
                likedByMe = !originalPost.likedByMe,
                likes = if (originalPost.likedByMe) originalPost.likes - 1
                else originalPost.likes + 1
            )
            dao.insert(updatedPost)

            val response = if (updatedPost.likedByMe) {
                PostsApi.service.likeById(id)
            } else {
                PostsApi.service.dislikeById(id)
            }

            if (!response.isSuccessful) {
                // Откатываем изменения при ошибке
                originalPost?.let { dao.insert(it) }
                throw ApiError(response.code(), response.message())
            }
        } catch (e: Exception) {
            originalPost?.let {
                try {
                    dao.insert(it)
                } catch (e: Exception) {
                    logger.severe("Failed to revert like changes: ${e.message}")
                }
            }

            when (e) {
                is IOException -> {
                    logger.severe("Network error while liking post: ${e.message}")
                    throw NetworkError
                }

                is ApiError -> throw e
                else -> {
                    logger.severe("Unknown error while liking post: ${e.message}")
                    throw UnknownError
                }
            }
        }
    }

    override fun getNewer(id: Long): Flow<Int> = flow {
        while (true) {
            delay(10_000)
            try {
                val latestId = dao.getLatestId() ?: id
                val response = PostsApi.service.getNewer(latestId)
                if (!response.isSuccessful) {
                    throw ApiError(response.code(), response.message())
                }

                val body = response.body() ?: throw ApiError(response.code(), response.message())
                val newPosts = body.filterNot { dao.exists(it.id) }

                if (newPosts.isNotEmpty()) {
                    dao.insert(newPosts.map { it.toEntity().copy(isHidden = true) })
                    emit(newPosts.size)
                }
            } catch (e: Exception) {
                logger.severe("Error in getNewer: ${e.message}")
                // Продолжаем работу после ошибки
            }
        }
    }.catch { e ->
        logger.severe("Flow error in getNewer: ${e.message}")
        throw AppError.from(e)
    }


    override suspend fun getLatestId(): Long? {
        return try {
            dao.getLatestId()
        } catch (e: Exception) {
            logger.severe("Failed to get latest ID: ${e.message}")
            null
        }
    }
}
