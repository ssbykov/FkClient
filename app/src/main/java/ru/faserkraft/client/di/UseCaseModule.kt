package ru.faserkraft.client.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.faserkraft.client.domain.usecase.dailyplan.GetDayPlansUseCase
import ru.faserkraft.client.domain.usecase.order.CloseOrderUseCase
import ru.faserkraft.client.domain.usecase.order.CreateOrderUseCase
import ru.faserkraft.client.domain.usecase.order.GetOrdersUseCase
import ru.faserkraft.client.domain.usecase.packaging.CreatePackagingUseCase
import ru.faserkraft.client.domain.usecase.packaging.GetPackagingInStorageUseCase
import ru.faserkraft.client.domain.usecase.packaging.GetPackagingUseCase
import ru.faserkraft.client.domain.usecase.product.ChangeProductProcessUseCase
import ru.faserkraft.client.domain.usecase.product.CompleteStepUseCase
import ru.faserkraft.client.domain.usecase.product.CreateProductUseCase
import ru.faserkraft.client.domain.usecase.product.GetFinishedProductsUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductUseCase
import ru.faserkraft.client.domain.usecase.product.UpdateProductStatusUseCase
import javax.inject.Singleton

/**
 * DI модуль для Use Cases
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideGetProductUseCase(
        repository: ru.faserkraft.client.domain.repository.ProductRepository
    ): GetProductUseCase {
        return GetProductUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideCreateProductUseCase(
        repository: ru.faserkraft.client.domain.repository.ProductRepository
    ): CreateProductUseCase {
        return CreateProductUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideUpdateProductStatusUseCase(
        repository: ru.faserkraft.client.domain.repository.ProductRepository
    ): UpdateProductStatusUseCase {
        return UpdateProductStatusUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideChangeProductProcessUseCase(
        repository: ru.faserkraft.client.domain.repository.ProductRepository
    ): ChangeProductProcessUseCase {
        return ChangeProductProcessUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideCompleteStepUseCase(
        repository: ru.faserkraft.client.domain.repository.ProductRepository
    ): CompleteStepUseCase {
        return CompleteStepUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetOrdersUseCase(
        repository: ru.faserkraft.client.domain.repository.OrderRepository
    ): GetOrdersUseCase {
        return GetOrdersUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideCreateOrderUseCase(
        repository: ru.faserkraft.client.domain.repository.OrderRepository
    ): CreateOrderUseCase {
        return CreateOrderUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideCloseOrderUseCase(
        repository: ru.faserkraft.client.domain.repository.OrderRepository
    ): CloseOrderUseCase {
        return CloseOrderUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetDayPlansUseCase(
        repository: ru.faserkraft.client.domain.repository.DailyPlanRepository
    ): GetDayPlansUseCase {
        return GetDayPlansUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetPackagingUseCase(
        repository: ru.faserkraft.client.domain.repository.PackagingRepository
    ): GetPackagingUseCase {
        return GetPackagingUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideCreatePackagingUseCase(
        repository: ru.faserkraft.client.domain.repository.PackagingRepository
    ): CreatePackagingUseCase {
        return CreatePackagingUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetPackagingInStorageUseCase(
        repository: ru.faserkraft.client.domain.repository.PackagingRepository
    ): GetPackagingInStorageUseCase {
        return GetPackagingInStorageUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetFinishedProductsUseCase(
        repository: ru.faserkraft.client.domain.repository.PackagingRepository
    ): GetFinishedProductsUseCase {
        return GetFinishedProductsUseCase(repository)
    }
}
