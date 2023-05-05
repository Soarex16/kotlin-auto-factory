package com.soarex.autofactory.kotlin.plugin.ir

import com.soarex.autofactory.kotlin.plugin.fir.CachingFactoryGenerator
import com.soarex.autofactory.kotlin.plugin.fir.Names
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.typeOperator
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.util.classIfConstructor
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.remapTypeParameters
import org.jetbrains.kotlin.name.Name

class TransformerForCompanionGenerator(context: IrPluginContext) : AbstractTransformerForGenerator(context, visitBodies = true) {
    private val getOrPutIrFunction by lazy {
        context
            .referenceFunctions(Names.GET_OR_PUT_CALLABLE_ID)
            .find { it.owner.extensionReceiverParameter?.type?.classFqName == Names.MUTABLE_MAP_FQN }!!
    }

    override fun interestedIn(key: GeneratedDeclarationKey?): Boolean {
        return key is CachingFactoryGenerator.Key
    }

    override fun generateBodyForFunction(function: IrSimpleFunction, key: GeneratedDeclarationKey?): IrBody {
        require(function.name.identifier == "create") // TODO: TEMP, refactor
        require(key is CachingFactoryGenerator.Key.CreateFunction)
        val constructor = function.returnType.getClass()!!
            .constructors
            .find { ctor ->
                // This need to distinguish (Foo<A>, Foo<B>) and (Foo<B>, Foo<A>)
                if (ctor.valueParameters.size != function.valueParameters.size) return@find false
                val typeMapping = ctor.classIfConstructor.typeParameters
                    .zip(function.typeParameters)
                    .toMap()
                val remappedCtorValueParamTypes = ctor.valueParameters
                    .map { it.type.remapTypeParameters(ctor, function, typeMapping) }

                remappedCtorValueParamTypes == function.valueParameters.map { it.type }
            }!!

        /*
         * fun <T> create(x: T): SomeTransformed<T> {
         *     return cache.getOrPut(Arguments_SecondaryCtor(x)) { SomeTransformed(x) } as SomeTransformed<T>
         * }
         */
        val builder = DeclarationIrBuilder(context, function.symbol)
        val factory = context.irFactory
        return builder.irBlockBody(function) {
            val correspondingDataClassConstructor = this@TransformerForCompanionGenerator.context
                .referenceConstructors(key.associatedConstructorKeyType)
                .first()
            val constructorKey = irCall(correspondingDataClassConstructor).apply {
                for ((i, vp) in function.valueParameters.withIndex()) {
                    putValueArgument(i, IrGetValueImpl(startOffset, endOffset, vp.type, vp.symbol))
                }

                for ((i, tp) in function.typeParameters.withIndex()) {
                    putTypeArgument(i, tp.defaultType)
                }
            }

            val starProjectedReturnType = function.returnType.getClass()!!.symbol.starProjectedType
            val defaultValueFactory = factory.buildFun {
                name = Name.special("<anonymous>")
                visibility = DescriptorVisibilities.LOCAL
                modality = Modality.FINAL
                returnType = starProjectedReturnType
                origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            }.apply {
                parent = function
                this.body = factory.createExpressionBody(
                    irCall(constructor).apply {
                        for ((i, vp) in function.valueParameters.withIndex()) {
                            putValueArgument(i, IrGetValueImpl(startOffset, endOffset, vp.type, vp.symbol))
                        }

                        for ((i, tp) in function.typeParameters.withIndex()) {
                            putTypeArgument(i, tp.defaultType)
                        }
                    }
                )
            }
            val function0Type =
                IrSimpleTypeImpl(context.irBuiltIns.functionN(0).symbol, false, emptyList(), emptyList())
            val defaultValueArgument = IrFunctionExpressionImpl(
                startOffset, endOffset, function0Type, defaultValueFactory,
                IrStatementOrigin.LAMBDA
            )

            val cacheProperty = function.parentAsClass.declarations
                .filterIsInstance<IrProperty>()
                .first { it.name == Names.OBJECT_CACHE }

            val getOrDefaultCall = irCall(getOrPutIrFunction).apply {
                extensionReceiver = irCall(cacheProperty.getter!!)
                val keyType = IrSimpleTypeImpl(
                    correspondingDataClassConstructor.owner.classIfConstructor.parentAsClass.symbol,
                    false,
                    emptyList(),
                    emptyList()
                )
                putTypeArgument(0, keyType)
                putValueArgument(0, constructorKey)

                putTypeArgument(1, starProjectedReturnType)
                putValueArgument(1, defaultValueArgument)
            }

            val cast = typeOperator(function.returnType, getOrDefaultCall, IrTypeOperator.CAST, function.returnType)

            +irReturn(cast)
        }
    }

    override fun generateBodyForConstructor(constructor: IrConstructor, key: GeneratedDeclarationKey?): IrBody? {
        return generateBodyForDefaultConstructor(constructor)
    }
}