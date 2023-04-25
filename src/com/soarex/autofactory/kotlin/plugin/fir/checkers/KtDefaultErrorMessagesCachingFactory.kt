package com.soarex.autofactory.kotlin.plugin.fir.checkers

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory

object KtDefaultErrorMessagesCachingFactory : BaseDiagnosticRendererFactory() {
    override val MAP = KtDiagnosticFactoryToRendererMap("CachingFactory").apply {
        put(
            PluginErrors.USELESS_IGNORE_ANNOTATION,
            "@IgnoreInCachingFactory annotation has no effect on constructor ''{0}'', because private constructors are ignored anyway",
        )

        put(
            PluginErrors.PRIMARY_CONSTRUCTOR_PARAMETER_SHOULD_BE_VAL,
            "Cached object primary constructor parameter should be 'val' because they are part of the factory cache key",
        )

        put(
            PluginErrors.NO_SUITABLE_CONSTRUCTORS,
            "Cannot create factory for class since it does not contain any public constructor not annotated with @IgnoreInCachingFactory",
        )
    }
}