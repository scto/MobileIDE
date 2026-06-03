cmake_minimum_required(VERSION 3.22.1)

project("webapp_crypto")

add_library(
        webapp_crypto
        SHARED
        native-lib.cpp)

find_library(
        log-lib
        log)

find_library(
        android-lib
        android)

target_link_libraries(
        webapp_crypto
        ${log-lib}
        ${android-lib})
