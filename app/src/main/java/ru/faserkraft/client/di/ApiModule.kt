package ru.faserkraft.client.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import ru.faserkraft.client.data.api.DailyPlanApi
import ru.faserkraft.client.data.api.OrderApi
import ru.faserkraft.client.data.api.PackagingApi
import ru.faserkraft.client.data.api.ProductApi
import javax.inject.Named
import javax.inject.Singleton

/**
 * DI модуль для API интерфейсов
 */
@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideProductApi(@Named("mainRetrofit") retrofit: Retrofit): ProductApi {
        return retrofit.create(ProductApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOrderApi(@Named("mainRetrofit") retrofit: Retrofit): OrderApi {
        return retrofit.create(OrderApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDailyPlanApi(@Named("mainRetrofit") retrofit: Retrofit): DailyPlanApi {
        return retrofit.create(DailyPlanApi::class.java)
    }

    @Provides
    @Singleton
    fun providePackagingApi(@Named("mainRetrofit") retrofit: Retrofit): PackagingApi {
        return retrofit.create(PackagingApi::class.java)
    }
}
