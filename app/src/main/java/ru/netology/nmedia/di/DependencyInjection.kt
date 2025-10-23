package ru.netology.nmedia.di

import android.content.Context
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.netology.nmedia.api.AuthApi
import ru.netology.nmedia.api.AuthApiService
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.api.PostsApiService
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DependencyInjection {

    // ========== DATABASE ==========
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDb {
        return AppDb.getInstance(context)
    }

    @Provides
    fun providePostDao(database: AppDb): PostDao {
        return database.postDao()
    }

    // ========== NETWORK ==========
    @Provides
    @Singleton
    fun providePostsApiService(): PostsApiService {
        return PostsApi.service
    }

    @Provides
    @Singleton
    fun provideAuthApiService(): AuthApiService {
        return AuthApi.service
    }

    // ========== REPOSITORIES ==========
    @Provides
    @Singleton
    fun providePostRepository(
        postDao: PostDao,
        postsApiService: PostsApiService
    ): PostRepository {
        return PostRepositoryImpl(postDao, postsApiService)
    }

    // ========== AUTH ==========
    @Provides
    @Singleton
    fun provideAppAuth(@ApplicationContext context: Context): AppAuth {
        AppAuth.initApp(context)
        return AppAuth.getInstance()
    }

    // ========== GOOGLE APIs ==========
    @Provides
    @Singleton
    fun provideGoogleApiAvailability(): GoogleApiAvailability {
        return GoogleApiAvailability.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging {
        return FirebaseMessaging.getInstance()
    }
}