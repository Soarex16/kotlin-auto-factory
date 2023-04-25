package com.soarex.autofactory.kotlin.plugin.fir.checkers

import com.soarex.autofactory.kotlin.plugin.fir.IgnoreInCachingFactoryClassId
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirConstructorChecker
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.visibility

object CachingFactoryConstructorChecker : FirConstructorChecker() {
    override fun check(declaration: FirConstructor, context: CheckerContext, reporter: DiagnosticReporter) {
        checkUselessIgnoreAnnotation(declaration, context, reporter)
        checkPrimaryConstructorParametersAreVal(declaration, reporter, context)
    }

    private fun checkPrimaryConstructorParametersAreVal(
        declaration: FirConstructor,
        reporter: DiagnosticReporter,
        context: CheckerContext
    ) {
        if (declaration.isPrimary && declaration.symbol.suitableForCachingFactory(context.session)) {
            for (valueParameter in declaration.valueParameters) {
                val correspondingProperty = valueParameter.correspondingProperty ?: return
                if (correspondingProperty.isVar) {
                    reporter.reportOn(
                        valueParameter.source,
                        PluginErrors.PRIMARY_CONSTRUCTOR_PARAMETER_SHOULD_BE_VAL,
                        context
                    )
                }
            }
        }
    }

    private fun checkUselessIgnoreAnnotation(
        declaration: FirConstructor, context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (Visibilities.isPrivate(declaration.visibility)) {
            val ignoreConstructorAnnotation =
                declaration.getAnnotationByClassId(IgnoreInCachingFactoryClassId, context.session)
            if (ignoreConstructorAnnotation != null) {
                reporter.reportOn(
                    ignoreConstructorAnnotation.source,
                    PluginErrors.USELESS_IGNORE_ANNOTATION,
                    context
                )
            }
        }
    }
}