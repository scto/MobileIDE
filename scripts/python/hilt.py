#!/usr/bin/env python3
import os
from pathlib import Path

def generate_hilt_module():
    package = "com.scto.mcside.core.template.di"
    file_path = Path("core/template/impl/src/main/java") / package.replace(".", "/") / "TemplateModule.kt"
    file_path.parent.mkdir(parents=True, exist_ok=True)

    # 🔧 FIX: Kotlin-Blöcke nutzen { } -> im Python f-String müssen sie als {{ }} escaped werden.
    content = f"""package {package}

import android.content.Context
import com.scto.mcside.core.template.api.ProjectLocationProvider
import com.scto.mcside.core.template.api.TemplateManager
import com.scto.mcside.core.template.api.TemplateQueryService
import com.scto.mcside.core.template.api.TemplateVersionRepository
import com.scto.mcside.core.template.data.DefaultTemplateVersionRepository
import com.scto.mcside.core.template.impl.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
annotation class IoDispatcher

@Qualifier
annotation class DefaultDispatcher

@Module
@InstallIn(SingletonComponent::class)
abstract class TemplateBindModule {{
    @Binds @Singleton
    abstract fun bindTemplateManager(impl: TemplateManagerImpl): TemplateManager

    @Binds @Singleton
    abstract fun bindQueryService(impl: TemplateQueryServiceImpl): TemplateQueryService

    @Binds @Singleton
    abstract fun bindVersionRepository(impl: DefaultTemplateVersionRepository): TemplateVersionRepository
}}

@Module@InstallIn(SingletonComponent::class)
object TemplateProvideModule {{

    @Provides @IoDispatcher @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO.limitedParallelism(2)

    @Provides @DefaultDispatcher @Singleton
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides @Singleton
    fun provideZipExtractor(@IoDispatcher dispatcher: CoroutineDispatcher): ZipExtractor = ZipExtractor(dispatcher)

    @Provides @Singleton
    fun provideTemplateDataSource(
        zipExtractor: ZipExtractor,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): TemplateDataSource = TemplateDataSource(zipExtractor, dispatcher)

    @Provides @Singleton
    fun provideGradleWrapperManager(
        versionRepo: TemplateVersionRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): GradleWrapperManager = GradleWrapperManager(versionRepo, dispatcher)

    @Provides @Singleton
    fun provideProjectGenerator(
        versionRepo: TemplateVersionRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): ProjectGenerator = ProjectGenerator(versionRepo, dispatcher)

    @Provides @Singleton
    fun provideTemplateManagerImpl(
        dataSource: TemplateDataSource,
        generator: ProjectGenerator,
        wrapperManager: GradleWrapperManager,
        locationProvider: ProjectLocationProvider,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): TemplateManagerImpl = TemplateManagerImpl(
        dataSource = dataSource,
        projectGenerator = generator,
        gradleWrapperManager = wrapperManager,
        locationProvider = locationProvider,
        ioDispatcher = dispatcher
    )
}}

// Hinweis: ProjectLocationProvider muss in einem Host-Modul (z.B. :app oder :core:domain) bereitgestellt werden.
// Beispiel:
// @Module @InstallIn(SingletonComponent::class)
// object LocationModule {{//     @Provides @Singleton fun provideLocationProvider(@ApplicationContext ctx: Context): ProjectLocationProvider = ...
// }}
"""

    with open(file_path, "w", encoding="utf-8") as f:
        f.write(content.strip() + "\n")
    print(f"✅ Hilt DI Module erfolgreich erstellt: {file_path}")

if __name__ == "__main__":
    generate_hilt_module()