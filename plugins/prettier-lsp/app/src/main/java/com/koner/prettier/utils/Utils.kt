package com.koner.prettier.utils

import com.koner.prettier.R
import com.scto.mobile.ide.features.extensions.ExtensionContext
import com.scto.mobile.ide.core.common.files.BuiltinFileType
import com.scto.mobile.ide.core.common.icons.Icon
import com.scto.mobile.ide.utils.isDarkTheme

val PRETTIER_EXTENSIONS =
    BuiltinFileType.JAVASCRIPT.extensions +
        BuiltinFileType.TYPESCRIPT.extensions +
        BuiltinFileType.JSX.extensions +
        BuiltinFileType.TSX.extensions +
        BuiltinFileType.CSS.extensions +
        BuiltinFileType.SCSS.extensions +
        BuiltinFileType.LESS.extensions +
        BuiltinFileType.HTML.extensions +
        BuiltinFileType.JSON.extensions +
        BuiltinFileType.MARKDOWN.extensions +
        BuiltinFileType.YAML.extensions +
        "vue" +
        "hbs" +
        "handlebars" +
        "graphql" +
        "gql" +
        "mdx" +
        "mjml"

fun getPrettierIcon(context: ExtensionContext): Icon.ExternalResourceIcon {
    val dark = isDarkTheme(context.appContext)
    val icon =
        if (dark) {
            R.drawable.prettier_icon_dark
        } else {
            R.drawable.prettier_icon_light
        }
    return Icon.ExternalResourceIcon(icon, context.resources)
}
