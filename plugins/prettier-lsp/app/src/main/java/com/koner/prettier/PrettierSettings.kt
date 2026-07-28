package com.koner.prettier

import com.scto.mobile.ide.features.extensions.ExtensionContext

class PrettierSettings(context: ExtensionContext) {

    // Indentation
    var useEditorDefault by context.settings.delegate("use_editor_default", true)
    var tabWidth by context.settings.delegate("tab_width", 2)
    var useTabs by context.settings.delegate("use_tabs", false)

    // JavaScript / TypeScript
    var semicolon by context.settings.delegate("semicolon", true)
    var singleQuote by context.settings.delegate("single_quote", false)
    var jsxSingleQuote by context.settings.delegate("jsx_single_quote", false)
    var quoteProps by context.settings.delegate("quote_props", "as-needed")
    var trailingComma by context.settings.delegate("trailing_comma", "all")
    var arrowParens by context.settings.delegate("arrow_parens", "always")

    // HTML
    var singleAttributePerLine by context.settings.delegate("single_attribute_per_line", false)
    var bracketSameLine by context.settings.delegate("bracket_same_line", false)

    // Layout
    var printWidth by context.settings.delegate("print_width", 80)
    var bracketSpacing by context.settings.delegate("bracket_spacing", true)
    var preserveObjectWrap by context.settings.delegate("preserve_object_wrap", true)

    // Advanced
    var customArgs by context.settings.delegate("custom_args", "")
}
