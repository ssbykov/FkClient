package ru.faserkraft.client.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.faserkraft.client.data.qr.QrClassifierImpl
import ru.faserkraft.client.data.repository.AuthRepositoryImpl
import ru.faserkraft.client.data.repository.DailyPlanRepositoryImpl
import ru.faserkraft.client.data.repository.DeviceRepositoryImpl
import ru.faserkraft.client.data.repository.EmployeeRepositoryImpl
import ru.faserkraft.client.data.repository.OrderRepositoryImpl
import ru.faserkraft.client.data.repository.PackagingRepositoryImpl
import ru.faserkraft.client.data.repository.ProcessRepositoryImpl
import ru.faserkraft.client.data.repository.ProductRepositoryImpl
import ru.faserkraft.client.data.repository.StepRepositoryImpl
import ru.faserkraft.client.domain.qr.QrClassifier
import ru.faserkraft.client.domain.repository.AuthRepository
import ru.faserkraft.client.domain.repository.DailyPlanRepository
import ru.faserkraft.client.domain.repository.DeviceRepository
import ru.faserkraft.client.domain.repository.EmployeeRepository
import ru.faserkraft.client.domain.repository.OrderRepository
import ru.faserkraft.client.domain.repository.PackagingRepository
import ru.faserkraft.client.domain.repository.ProcessRepository
import ru.faserkraft.client.domain.repository.ProductRepository
import ru.faserkraft.client.domain.repository.StepRepository
import ru.faserkraft.client.domain.repository.UpdateRepository
import ru.faserkraft.client.data.repository.UpdateRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

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

    @Binds
    abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository

    @Binds
    @Singleton
    abstract fun bindQrClassifier(impl: QrClassifierImpl): QrClassifier

    @Binds
    @Singleton
    abstract fun bindsDaoRepository(impl: UpdateRepositoryImpl): UpdateRepository
}