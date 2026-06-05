cmake_minimum_required(VERSION 3.22.1)

project(language_treesitter C)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

include(FetchContent)

# Avoid updating sources every build (predictable + reproducible)
set(FETCHCONTENT_UPDATES_DISCONNECTED ON)

# Windows default MAX_PATH(260) is easy to hit with FetchContent's sub-build layout.
# Put FetchContent working dirs under a shorter path (module root) to avoid it.
get_filename_component(_module_root "${CMAKE_CURRENT_LIST_DIR}/../../.." ABSOLUTE)
set(FETCHCONTENT_BASE_DIR "${_module_root}/.fc")

macro(fetch_grammar grammar_name repo tag)
    FetchContent_Declare(${grammar_name}
        GIT_REPOSITORY ${repo}
        GIT_TAG ${tag}
        GIT_SHALLOW TRUE
    )

    FetchContent_GetProperties(${grammar_name})
    if(NOT ${grammar_name}_POPULATED)
        FetchContent_Populate(${grammar_name})
    endif()
endmacro()

macro(add_treesitter_lang lib_target grammar_name repo tag jni_source)
    fetch_grammar(${grammar_name} ${repo} ${tag})

    set(lang_src_dir ${${grammar_name}_SOURCE_DIR}/src)

    set(lang_sources ${lang_src_dir}/parser.c)
    if(EXISTS ${lang_src_dir}/scanner.c)
        list(APPEND lang_sources ${lang_src_dir}/scanner.c)
    endif()

    add_library(${lib_target} SHARED
        ${lang_sources}
        ${CMAKE_CURRENT_LIST_DIR}/jni/${jni_source}
    )

    target_include_directories(${lib_target} PRIVATE ${lang_src_dir})

    target_compile_options(${lib_target} PRIVATE
        -Wall
        -Wextra
        -Wno-unused-parameter
        -Wno-sign-compare
    )
endmacro()

add_treesitter_lang(
    tree-sitter-html
    ts_html
    https://github.com/tree-sitter/tree-sitter-html.git
    v0.23.2
    ts_html_jni.c
)

add_treesitter_lang(
    tree-sitter-css
    ts_css
    https://github.com/tree-sitter/tree-sitter-css.git
    v0.25.0
    ts_css_jni.c
)

add_treesitter_lang(
    tree-sitter-javascript
    ts_js
    https://github.com/tree-sitter/tree-sitter-javascript.git
    v0.25.0
    ts_javascript_jni.c
)