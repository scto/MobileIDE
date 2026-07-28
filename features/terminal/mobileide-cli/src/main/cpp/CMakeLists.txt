cmake_minimum_required(VERSION 3.22.1)
project("mobileide_cli")

add_executable(${CMAKE_PROJECT_NAME}
        mobileide_cli.cpp)

target_compile_options(${CMAKE_PROJECT_NAME} PRIVATE
        "-ffile-prefix-map=${CMAKE_CURRENT_SOURCE_DIR}=."
        "-ffile-prefix-map=${CMAKE_CURRENT_BINARY_DIR}=."
        "-g0"
        "-fno-ident"
        "-fno-exceptions"
        "-fno-rtti"
        "-Oz"
        "-DNDEBUG")

# Added "-static" to link standard libraries statically
target_link_options(${CMAKE_PROJECT_NAME} PRIVATE
        "-static"
        "-Wl,-s"
        "-Wl,--build-id=none"
        "-Wl,--gc-sections"
        "-Wl,-z,noexecstack")

set_target_properties(${CMAKE_PROJECT_NAME} PROPERTIES
        OUTPUT_NAME "mobileide_cli"
        PREFIX "lib"
        SUFFIX ".so"
)