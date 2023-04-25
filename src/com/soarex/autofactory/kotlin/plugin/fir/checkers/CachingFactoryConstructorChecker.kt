package com.soarex.autofactory.kotlin.plugin.fir.checkers

import com.soarex.autofactory.kotlin.plugin.fir.IgnoreInCachingFactoryClassId
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirConstructorChecker
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.visibility

object CachingFactoryConstructorChecker : FirConstructorChecker() {
    override fun check(declaration: FirConstructor, context: CheckerContext, reporter: DiagnosticReporter) {
        if (Visibilities.isPrivate(declaration.visibility)) {
            val ignoreConstructorAnnotationSource =
                declaration.getAnnotationByClassId(IgnoreInCachingFactoryClassId, context.session)?.source ?: return
            reporter.reportOn(
                ignoreConstructorAnnotationSource,
                PluginErrors.USELESS_IGNORE_ANNOTATION,
                context
            )
        }
    }
}