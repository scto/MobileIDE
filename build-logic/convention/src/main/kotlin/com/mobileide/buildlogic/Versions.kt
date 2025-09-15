package com.mobileide.buildlogic

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

object Versions {
    const val targetSdk = 35
    const val compileSdk = 35
    const val minSdk = 29

    val javaSourceCompatibility = JavaVersion.VERSION_17
    val javaTargetCompatibility = JavaVersion.VERSION_17
    
    val kotlinJvmTarget = JvmTarget.JVM_17
}