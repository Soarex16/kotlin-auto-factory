package com.soarex.autofactory.kotlin.plugin.fir.checkers

import com.soarex.autofactory.kotlin.plugin.fir.IgnoreInCachingFactoryClassId
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.constructors
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.visibility

object CachingFactoryClassChecker : FirClassChecker() {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.hasConstructorsSuitableForCachingFactory(context.session)) {
            reporter.reportOn(
                declaration.source,
                PluginErrors.NO_SUITABLE_CONSTRUCTORS,
                context,
                positioningStrategy = SourceElementPositioningStrategies.ENUM_MODIFIER,
            )
        }
    }

    private fun FirClass.hasConstructorsSuitableForCachingFactory(session: FirSession): Boolean {
        return constructors(session)
            .any { !Visibilities.isPrivate(it.visibility) && !it.hasAnnotation(IgnoreInCachingFactoryClassId, session) }
    }
}