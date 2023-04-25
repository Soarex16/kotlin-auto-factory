package com.soarex.autofactory.kotlin.plugin

import com.soarex.autofactory.kotlin.plugin.fir.CachingFactoryConstructorStatusTransformer
import com.soarex.autofactory.kotlin.plugin.fir.CachingFactoryGenerator
import com.soarex.autofactory.kotlin.plugin.fir.CachingFactoryInfoProvider
import com.soarex.autofactory.kotlin.plugin.fir.checkers.FirCachingFactoryCheckersComponent
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class AutoFactoryPluginRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::CachingFactoryGenerator
        +::CachingFactoryConstructorStatusTransformer

        +::FirCachingFactoryCheckersComponent

        +::CachingFactoryInfoProvider
    }
}
