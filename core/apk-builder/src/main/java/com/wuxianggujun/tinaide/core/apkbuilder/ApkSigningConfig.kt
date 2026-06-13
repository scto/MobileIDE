package com.wuxianggujun.tinaide.core.apkbuilder

sealed interface ApkSigningConfig {
    data object Debug : ApkSigningConfig

    data class Custom(
        val keyStoreInfo: DebugKeyStore.KeyStoreInfo
    ) : ApkSigningConfig
}
