package ru.faserkraft.client.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.faserkraft.client.utils.QrCodeGeneratorWrapper
import ru.faserkraft.client.utils.QrCodeGeneratorWrapperImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class UtilsModule {
    @Binds
    abstract fun bindQrCodeGenerator(
        impl: QrCodeGeneratorWrapperImpl
    ): QrCodeGeneratorWrapper
}