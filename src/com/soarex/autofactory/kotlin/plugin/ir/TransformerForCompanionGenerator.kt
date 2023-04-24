package com.soarex.autofactory.kotlin.plugin.ir

import com.soarex.autofactory.kotlin.plugin.fir.CachingFactoryGenerator
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody

class TransformerForCompanionGenerator(context: IrPluginContext) : AbstractTransformerForGenerator(context, visitBodies = true) {
    override fun interestedIn(key: GeneratedDeclarationKey?): Boolean {
        return key == CachingFactoryGenerator.Key
    }

    override fun generateBodyForFunction(function: IrSimpleFunction, key: GeneratedDeclarationKey?): IrBody? {
        return null
    }

    override fun generateBodyForConstructor(constructor: IrConstructor, key: GeneratedDeclarationKey?): IrBody? {
        return generateBodyForDefaultConstructor(constructor)
    }
}