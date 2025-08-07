package ru.netology.nmedia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.entity.PostEntity

@Dao
interface PostDao {
    // Основной запрос (возвращает все посты)
    @Query("SELECT * FROM posts ORDER BY id DESC")
    fun getAll(): Flow<List<PostEntity>>

    // Запрос только видимых постов
    @Query("SELECT * FROM posts WHERE isHidden = 0 ORDER BY id DESC")
    fun getAllVisible(): Flow<List<PostEntity>>

    @Query("SELECT COUNT(*) == 0 FROM posts")
    suspend fun isEmpty(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(posts: List<PostEntity>)

    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getById(id: Long): PostEntity?

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun removeById(id: Long)

    // Новые методы для функционала "New Posts"
    @Query("SELECT COUNT(*) FROM posts WHERE isHidden = 1")
    fun getHiddenCount(): Flow<Int>

    @Query("UPDATE posts SET isHidden = 0 WHERE isHidden = 1")
    suspend fun showAllHiddenPosts()

    @Query("SELECT MAX(id) FROM posts")
    suspend fun getLatestId(): Long?

    // Для проверки существования поста перед добавлением
    @Query("SELECT EXISTS(SELECT * FROM posts WHERE id = :id)")
    suspend fun exists(id: Long): Boolean
}