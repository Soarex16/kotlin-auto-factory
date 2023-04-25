package com.soarex.autofactory.kotlin.plugin.fir.checkers

import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.warning0
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass

object PluginErrors {
    val USELESS_IGNORE_ANNOTATION by warning0<KtAnnotationEntry>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val NO_SUITABLE_CONSTRUCTORS by error0<KtClass>(SourceElementPositioningStrategies.DECLARATION_NAME)

    init {
        RootDiagnosticRendererFactory.registerFactory(KtDefaultErrorMessagesCachingFactory)
    }
}