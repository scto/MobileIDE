package com.scto.mobile.ide.lsp

sealed interface LspConnectionConfig {
    data class Socket(val host: String = "localhost", val port: Int) : LspConnectionConfig

    data class Process(val command: Array<String>) : LspConnectionConfig {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Process

            return command.contentEquals(other.command)
        }

        override fun hashCode(): Int {
            return command.contentHashCode()
        }
    }

    data class AndroidProcess(val command: Array<String>) : LspConnectionConfig {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AndroidProcess

            return command.contentEquals(other.command)
        }

        override fun hashCode(): Int {
            return command.contentHashCode()
        }
    }
}
