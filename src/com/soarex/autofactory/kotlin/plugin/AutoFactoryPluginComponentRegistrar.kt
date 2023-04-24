package com.soarex.autofactory.kotlin.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import com.soarex.autofactory.kotlin.plugin.ir.AutoFactoryIrGenerationExtension

class AutoFactoryPluginComponentRegistrar: CompilerPluginRegistrar() {
    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(AutoFactoryPluginRegistrar())
        IrGenerationExtension.registerExtension(AutoFactoryIrGenerationExtension())
    }
}
