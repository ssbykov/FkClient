package ru.faserkraft.client.data.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.faserkraft.client.domain.repository.DailyPlanRepository
import ru.faserkraft.client.domain.repository.EmployeeRepository
import ru.faserkraft.client.domain.repository.OrderRepository
import ru.faserkraft.client.domain.repository.PackagingRepository
import ru.faserkraft.client.domain.repository.ProcessRepository
import ru.faserkraft.client.domain.repository.ProductRepository
import ru.faserkraft.client.domain.repository.StepRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DomainRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds
    @Singleton
    abstract fun bindStepRepository(impl: StepRepositoryImpl): StepRepository

    @Binds
    @Singleton
    abstract fun bindProcessRepository(impl: ProcessRepositoryImpl): ProcessRepository

    @Binds
    @Singleton
    abstract fun bindEmployeeRepository(impl: EmployeeRepositoryImpl): EmployeeRepository

    @Binds
    @Singleton
    abstract fun bindPackagingRepository(impl: PackagingRepositoryImpl): PackagingRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository

    @Binds
    @Singleton
    abstract fun bindDailyPlanRepository(impl: DailyPlanRepositoryImpl): DailyPlanRepository
}