package com.scto.mobile.ide.features.layoutpreview

data class ComposablePreviewTarget(
    val functionName: String,
    val hasPreviewAnnotation: Boolean,
    val lineNumber: Int
)

object ComposePreviewScanner {

    private val composableRegex = Regex(
        "(?m)(@Preview\\s+)?@Composable\\s+(?:private\\s+|protected\\s+|internal\\s+)?fun\\s+([a-zA-Z0-9_]+)\\s*\\(\\s*\\)"
    )

    fun scan(fileText: String): List<ComposablePreviewTarget> {
        if (fileText.isBlank() || !fileText.contains("@Composable")) return emptyList()

        val results = mutableListOf<ComposablePreviewTarget>()
        val lines = fileText.lines()

        composableRegex.findAll(fileText).forEach { matchResult ->
            val hasPreview = matchResult.groupValues[1].isNotBlank()
            val funName = matchResult.groupValues[2]

            // Find line number
            val charIndex = matchResult.range.first
            var lineNum = 1
            var count = 0
            for (line in lines) {
                count += line.length + 1
                if (count > charIndex) break
                lineNum++
            }

            results.add(
                ComposablePreviewTarget(
                    functionName = funName,
                    hasPreviewAnnotation = hasPreview,
                    lineNumber = lineNum
                )
            )
        }

        return results.distinctBy { it.functionName }
    }
}
