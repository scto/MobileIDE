package com.mobileide.templates.di

import com.mobileide.templates.data.TemplateRepository
import com.mobileide.templates.data.TemplateRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TemplatesModule {

    @Binds
    @Singleton
    abstract fun bindTemplateRepository(
        impl: TemplateRepositoryImpl
    ): TemplateRepository
}
