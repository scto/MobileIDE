package com.mobileide.svgtoavd.di

import com.mobileide.svgtoavd.data.SvgConverterRepository
import com.mobileide.svgtoavd.data.SvgConverterRepositoryImpl

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SvgToAvdModule {

    @Binds
    @Singleton
    abstract fun bindSvgConverterRepository(
        impl: SvgConverterRepositoryImpl
    ): SvgConverterRepository
}
