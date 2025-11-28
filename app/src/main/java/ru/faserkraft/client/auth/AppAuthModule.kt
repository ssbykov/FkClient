package ru.faserkraft.client.auth

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
interface AppAuthModule {
    @Singleton
    @Binds
    fun bindsApiRepository(impl: AppAuthImpl): AppAuth
}