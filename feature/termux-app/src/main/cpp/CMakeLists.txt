cmake_minimum_required(VERSION 3.22.1)

project("termux-app")

add_library(
    termux-app
    SHARED
    native-lib.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    termux-app
    ${log-lib}
)
