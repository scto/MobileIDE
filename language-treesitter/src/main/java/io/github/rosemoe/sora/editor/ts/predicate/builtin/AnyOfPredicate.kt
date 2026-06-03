package io.github.rosemoe.sora.editor.ts.predicate.builtin

import com.itsaky.androidide.treesitter.TSQuery
import com.itsaky.androidide.treesitter.TSQueryMatch
import com.itsaky.androidide.treesitter.TSQueryPredicateStep.Type
import io.github.rosemoe.sora.editor.ts.predicate.PredicateResult
import io.github.rosemoe.sora.editor.ts.predicate.TsClientPredicateStep
import io.github.rosemoe.sora.editor.ts.predicate.TsPredicate
import io.github.rosemoe.sora.editor.ts.predicate.TsSyntheticCaptureContainer

object AnyOfPredicate : TsPredicate {

    override fun doPredicate(
        tsQuery: TSQuery,
        text: CharSequence,
        match: TSQueryMatch,
        predicateSteps: List<TsClientPredicateStep>,
        syntheticCaptures: TsSyntheticCaptureContainer
    ): PredicateResult {
        if (predicateSteps.isEmpty()) return PredicateResult.UNHANDLED
        val name = predicateSteps[0].content
        if (name != "any-of?" && name != "any_of?") return PredicateResult.UNHANDLED
        if (predicateSteps.size < 4) return PredicateResult.UNHANDLED
        if (predicateSteps[1].predicateType != Type.Capture) return PredicateResult.UNHANDLED

        val candidates = LinkedHashSet<String>(predicateSteps.size)
        for (i in 2 until predicateSteps.size) {
            val step = predicateSteps[i]
            if (step.predicateType == Type.Done) break
            if (step.predicateType == Type.String) {
                candidates.add(step.content)
            }
        }
        if (candidates.isEmpty()) return PredicateResult.UNHANDLED

        val captured = getCaptureContent(tsQuery, match, predicateSteps[1].content, text)
        for (value in captured) {
            if (value !in candidates) {
                return PredicateResult.REJECT
            }
        }
        return PredicateResult.ACCEPT
    }
}

