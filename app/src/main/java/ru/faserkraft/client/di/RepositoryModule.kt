package ru.faserkraft.client.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.faserkraft.client.data.repository.DailyPlanRepositoryImpl
import ru.faserkraft.client.data.repository.OrderRepositoryImpl
import ru.faserkraft.client.data.repository.PackagingRepositoryImpl
import ru.faserkraft.client.data.repository.ProductRepositoryImpl
import ru.faserkraft.client.domain.repository.DailyPlanRepository
import ru.faserkraft.client.domain.repository.OrderRepository
import ru.faserkraft.client.domain.repository.PackagingRepository
import ru.faserkraft.client.domain.repository.ProductRepository

/**
 * DI модуль для Repository
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindProductRepository(
        impl: ProductRepositoryImpl
    ): ProductRepository

    @Binds
    abstract fun bindOrderRepository(
        impl: OrderRepositoryImpl
    ): OrderRepository

    @Binds
    abstract fun bindDailyPlanRepository(
        impl: DailyPlanRepositoryImpl
    ): DailyPlanRepository

    @Binds
    abstract fun bindPackagingRepository(
        impl: PackagingRepositoryImpl
    ): PackagingRepository
}