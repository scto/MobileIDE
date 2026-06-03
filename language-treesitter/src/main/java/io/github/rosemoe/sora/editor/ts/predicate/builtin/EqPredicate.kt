package io.github.rosemoe.sora.editor.ts.predicate.builtin

import com.itsaky.androidide.treesitter.TSQuery
import com.itsaky.androidide.treesitter.TSQueryMatch
import com.itsaky.androidide.treesitter.TSQueryPredicateStep.Type
import io.github.rosemoe.sora.editor.ts.predicate.PredicateResult
import io.github.rosemoe.sora.editor.ts.predicate.TsClientPredicateStep
import io.github.rosemoe.sora.editor.ts.predicate.TsPredicate
import io.github.rosemoe.sora.editor.ts.predicate.TsSyntheticCaptureContainer

object EqPredicate : TsPredicate {

    private val PARAMETERS = arrayOf(Type.String, Type.Capture, Type.String, Type.Done)

    override fun doPredicate(
        tsQuery: TSQuery,
        text: CharSequence,
        match: TSQueryMatch,
        predicateSteps: List<TsClientPredicateStep>,
        syntheticCaptures: TsSyntheticCaptureContainer
    ): PredicateResult {
        if (!parametersMatch(predicateSteps, PARAMETERS)) return PredicateResult.UNHANDLED
        if (predicateSteps[0].content != "eq?") return PredicateResult.UNHANDLED

        val expected = predicateSteps[2].content
        val captured = getCaptureContent(tsQuery, match, predicateSteps[1].content, text)
        for (value in captured) {
            if (value != expected) {
                return PredicateResult.REJECT
            }
        }
        return PredicateResult.ACCEPT
    }
}

