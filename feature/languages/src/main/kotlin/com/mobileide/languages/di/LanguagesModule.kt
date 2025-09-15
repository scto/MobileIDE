package com.mobileide.languages.di

import com.mobileide.languages.LanguageServerDefinition
import com.mobileide.languages.servers.*

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class LanguagesModule {

    @Binds
    @IntoSet
    abstract fun bindKotlinLanguageServer(server: KotlinLanguageServer): LanguageServerDefinition

    @Binds
    @IntoSet
    abstract fun bindJavaLanguageServer(server: JavaLanguageServer): LanguageServerDefinition
    
    @Binds
    @IntoSet
    abstract fun bindXmlLanguageServer(server: XmlLanguageServer): LanguageServerDefinition

    @Binds
    @IntoSet
    abstract fun bindGradleKotlinLanguageServer(server: GradleKotlinLanguageServer): LanguageServerDefinition
    
    @Binds
    @IntoSet
    abstract fun bindGradleGroovyLanguageServer(server: GradleGroovyLanguageServer): LanguageServerDefinition

    @Binds
    @IntoSet
    abstract fun bindJsonLanguageServer(server: JsonLanguageServer): LanguageServerDefinition

    @Binds
    @IntoSet
    abstract fun bindGitLanguageServer(server: GitLanguageServer): LanguageServerDefinition

    @Binds
    @IntoSet
    abstract fun bindPropertiesLanguageServer(server: PropertiesLanguageServer): LanguageServerDefinition
}
