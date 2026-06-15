package com.ixeken.worldcupinfo.di.network

import com.ixeken.worldcupinfo.data.remote.api.MatchApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Cache
import java.io.File
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context
    ): OkHttpClient {
        val cacheSize = 10 * 1024 * 1024L // 10 MB
        val cache = Cache(context.cacheDir, cacheSize)
        return OkHttpClient.Builder()
            .cache(cache)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideMatchApiService(retrofit: Retrofit): MatchApiService {
        return retrofit.create(MatchApiService::class.java)
    }
}
