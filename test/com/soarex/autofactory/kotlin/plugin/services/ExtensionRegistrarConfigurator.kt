package com.soarex.autofactory.kotlin.plugin.services

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import com.soarex.autofactory.kotlin.plugin.AutoFactoryPluginRegistrar
import com.soarex.autofactory.kotlin.plugin.ir.AutoFactoryIrGenerationExtension

class ExtensionRegistrarConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {
        FirExtensionRegistrarAdapter.registerExtension(AutoFactoryPluginRegistrar())
        IrGenerationExtension.registerExtension(AutoFactoryIrGenerationExtension())
    }
}
