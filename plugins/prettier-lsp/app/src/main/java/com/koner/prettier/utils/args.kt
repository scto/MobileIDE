package com.koner.prettier.utils

import com.koner.prettier.PrettierSettings
import com.scto.mobile.ide.settings.Settings

fun buildArgs(settings: PrettierSettings): Array<String> {
    return buildList {
        add("--config-precedence=prefer-file")

        if (!Settings.enable_editorconfig) {
            add("--no-editorconfig")
        }

        if (settings.useEditorDefault) {
            add("--tab-width=${Settings.tab_size}")

            if (Settings.actual_tabs) {
                add("--use-tabs")
            }
        } else {
            add("--tab-width=${settings.tabWidth}")

            if (settings.useTabs) {
                add("--use-tabs")
            }
        }

        if (!settings.semicolon) {
            add("--no-semi")
        }

        if (settings.singleQuote) {
            add("--single-quote")
        }

        if (settings.jsxSingleQuote) {
            add("--jsx-single-quote")
        }

        add("--quote-props=${settings.quoteProps}")

        if (settings.preserveObjectWrap) {
            add("--object-wrap=preserve")
        } else {
            add("--object-wrap=collapse")
        }

        if (settings.singleAttributePerLine) {
            add("--single-attribute-per-line")
        }

        if (settings.bracketSameLine) {
            add("--bracket-same-line")
        }

        add("--trailing-comma=${settings.trailingComma}")

        add("--print-width=${settings.printWidth}")

        if (!settings.bracketSpacing) {
            add("--no-bracket-spacing")
        }

        add("--arrow-parens=${settings.arrowParens}")

        if (settings.customArgs.isNotBlank()) {
            addAll(settings.customArgs.trim().split(" ").filter { it.isNotBlank() })
        }
    }
        .toTypedArray()
}
