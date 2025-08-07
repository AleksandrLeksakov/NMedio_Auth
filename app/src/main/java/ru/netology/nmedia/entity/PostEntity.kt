package ru.netology.nmedia.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Post

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val author: String,
    val authorAvatar: String,
    val content: String,
    val published: String,
    val likedByMe: Boolean,
    val likes: Int = 0,
    val isHidden: Boolean = false
) {
    fun toDto(): Post = Post(
        id = id,
        author = author,
        authorAvatar = authorAvatar,
        content = content,
        published = published,
        likedByMe = likedByMe,
        likes = likes,
        isHidden = isHidden
    )

    companion object {
        fun fromDto(dto: Post): PostEntity = PostEntity(
            id = dto.id,
            author = dto.author,
            authorAvatar = dto.authorAvatar,
            content = dto.content,
            published = dto.published,
            likedByMe = dto.likedByMe,
            likes = dto.likes,
            isHidden = dto.isHidden ?: false
        )
    }
}

// Extension-функции для преобразования списков
fun List<PostEntity>.toDto(): List<Post> = map { it.toDto() }

fun List<Post>.toEntity(): List<PostEntity> = map { PostEntity.fromDto(it) }

// Дополнительная функция для преобразования одного поста
fun Post.toEntity(): PostEntity = PostEntity.fromDto(this)