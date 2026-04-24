package ru.faserkraft.client.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@InstallIn(SingletonComponent::class)
@Module
interface UpdateRepositoryModule {

    @Singleton
    @Binds
    fun bindsDaoRepository(impl: UpdateRepositoryImpl): UpdateRepository
}