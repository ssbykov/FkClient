package ru.faserkraft.client.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.faserkraft.client.auth.AppAuth
import ru.faserkraft.client.auth.AppAuthImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppAuthModule {

    @Binds
    @Singleton
    abstract fun bindAppAuth(impl: AppAuthImpl): AppAuth
}