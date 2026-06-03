package io.github.rosemoe.sora.editor.ts.predicate.builtin

import com.itsaky.androidide.treesitter.TSQuery
import com.itsaky.androidide.treesitter.TSQueryMatch
import com.itsaky.androidide.treesitter.TSQueryPredicateStep.Type
import io.github.rosemoe.sora.editor.ts.predicate.PredicateResult
import io.github.rosemoe.sora.editor.ts.predicate.TsClientPredicateStep
import io.github.rosemoe.sora.editor.ts.predicate.TsPredicate
import io.github.rosemoe.sora.editor.ts.predicate.TsSyntheticCaptureContainer
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.PatternSyntaxException

object LuaMatchPredicate : TsPredicate {

    private val PARAMETERS = arrayOf(Type.String, Type.Capture, Type.String, Type.Done)
    private val cache = ConcurrentHashMap<String, Regex>()

    override fun doPredicate(
        tsQuery: TSQuery,
        text: CharSequence,
        match: TSQueryMatch,
        predicateSteps: List<TsClientPredicateStep>,
        syntheticCaptures: TsSyntheticCaptureContainer
    ): PredicateResult {
        if (!parametersMatch(predicateSteps, PARAMETERS)) return PredicateResult.UNHANDLED
        val name = predicateSteps[0].content
        if (name != "lua-match?" && name != "lua_match?") return PredicateResult.UNHANDLED

        val captured = getCaptureContent(tsQuery, match, predicateSteps[1].content, text)
        val luaPattern = predicateSteps[2].content
        try {
            val regex = cache.getOrPut(luaPattern) { Regex(luaPatternToJavaRegex(luaPattern)) }
            for (value in captured) {
                if (regex.find(value) == null) {
                    return PredicateResult.REJECT
                }
            }
            return PredicateResult.ACCEPT
        } catch (e: PatternSyntaxException) {
            e.printStackTrace()
            return PredicateResult.UNHANDLED
        }
    }

    private fun luaPatternToJavaRegex(pattern: String): String {
        if (!pattern.contains('%')) return pattern

        val out = StringBuilder(pattern.length + 8)
        var i = 0
        while (i < pattern.length) {
            val ch = pattern[i]
            if (ch != '%') {
                out.append(ch)
                i++
                continue
            }

            if (i + 1 >= pattern.length) {
                out.append('%')
                i++
                continue
            }

            val next = pattern[i + 1]
            val replacement = luaClassToRegex(next)
            if (replacement != null) {
                out.append(replacement)
                i += 2
                continue
            }

            if (next == '%') {
                out.append('%')
                i += 2
                continue
            }

            out.append('\\')
            out.append(next)
            i += 2
        }
        return out.toString()
    }

    private fun luaClassToRegex(c: Char): String? = when (c) {
        'a' -> "\\p{Alpha}"
        'A' -> "\\P{Alpha}"
        'c' -> "\\p{Cntrl}"
        'C' -> "\\P{Cntrl}"
        'd' -> "\\d"
        'D' -> "\\D"
        'l' -> "\\p{Ll}"
        'L' -> "\\P{Ll}"
        'p' -> "\\p{Punct}"
        'P' -> "\\P{Punct}"
        's' -> "\\s"
        'S' -> "\\S"
        'u' -> "\\p{Lu}"
        'U' -> "\\P{Lu}"
        'w' -> "\\p{Alnum}"
        'W' -> "\\P{Alnum}"
        'x' -> "\\p{XDigit}"
        'X' -> "\\P{XDigit}"
        else -> null
    }
}

