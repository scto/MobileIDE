package com.scto.mobile.ide.core.tooling.api

/**
 * IPC protocol constants shared between the Linux Gradle server and the Android client.
 *
 * Socket: /tmp/mobileide_tooling.sock
 *
 * Protocol (newline-delimited):
 *   Client → Server:
 *     GET_TASKS|<projectPath>
 *     EXECUTE_TASKS|<projectPath>|task1,task2,...
 *
 *   Server → Client:
 *     TASKS_LIST|task1,task2,...
 *     PROGRESS|<message>
 *     LOG|INFO|<text>
 *     LOG|WARN|<text>
 *     LOG|ERROR|<text>
 *     LOG|DEBUG|<text>
 *     RESULT|SUCCESS
 *     RESULT|ERROR
 *     ERROR|<message>       (fatal server error, connection will close)
 */
object IpcProtocol {
    const val SOCKET_PATH = "/tmp/mobileide_tooling.sock"

    // Client → Server commands
    const val CMD_GET_TASKS = "GET_TASKS"
    const val CMD_EXECUTE_TASKS = "EXECUTE_TASKS"

    // Server → Client prefixes
    const val RESP_TASKS_LIST = "TASKS_LIST"
    const val RESP_PROGRESS = "PROGRESS"
    const val RESP_LOG = "LOG"
    const val RESP_RESULT = "RESULT"
    const val RESP_ERROR = "ERROR"

    // Result values
    const val RESULT_SUCCESS = "SUCCESS"
    const val RESULT_ERROR = "ERROR"

    // Log levels
    const val LEVEL_INFO = "INFO"
    const val LEVEL_WARN = "WARN"
    const val LEVEL_ERROR = "ERROR"
    const val LEVEL_DEBUG = "DEBUG"
    const val LEVEL_QUIET = "QUIET"

    const val DELIMITER = "|"
    const val LIST_SEPARATOR = ","
}
