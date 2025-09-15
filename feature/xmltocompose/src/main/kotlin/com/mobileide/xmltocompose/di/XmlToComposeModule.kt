package com.mobileide.xmltocompose.di

import com.mobileide.xmltocompose.data.XmlConverterRepository
import com.mobileide.xmltocompose.data.XmlConverterRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class XmlToComposeModule {

    @Binds
    @Singleton
    abstract fun bindXmlConverterRepository(
        impl: XmlConverterRepositoryImpl
    ): XmlConverterRepository
}
