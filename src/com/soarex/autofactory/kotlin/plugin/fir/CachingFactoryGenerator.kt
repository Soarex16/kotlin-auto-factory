package com.soarex.autofactory.kotlin.plugin.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.plugin.*
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

class CachingFactoryGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext
    ): FirClassLikeSymbol<*>? {
        @Suppress("NAME_SHADOWING")
        val owner = session.symbolProvider
            .getClassLikeSymbolByClassId(owner.classId) as? FirRegularClassSymbol ?: return null
        return when (name) {
            Names.CONSTRUCTOR_ARGUMENTS_BASE_CLASS -> {
                createNestedClass(owner, Names.CONSTRUCTOR_ARGUMENTS_BASE_CLASS, Key, ClassKind.CLASS) {
                    modality = Modality.SEALED
                    visibility = Visibilities.Private
                }.symbol
            }

            SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT -> {
                if (owner.companionObjectSymbol == null) {
                    createCompanionObject(owner, Key).symbol
                } else {
                    null
                }
            }

            else -> {
                if (owner.classId.shortClassName == Names.CONSTRUCTOR_ARGUMENTS_BASE_CLASS) {
                    val sourceDataClassClassId = context
                        .owner.classId // ConstructorArgumentsKey base class
                        .parentClassId // companion
                        ?.parentClassId // data class
                    val associatedConstructor = session.cachingFactoryInfoProvider
                        .getAssociatedConstructors(sourceDataClassClassId)
                        ?.get(name)!!
                    createNestedClass(owner, name, Key, ClassKind.CLASS) {
                        modality = Modality.SEALED
                        status {
                            isData = true
                        }
                        superType(owner.classId.defaultType(owner.typeParameterSymbols))

                        for (typeParamSymbol in associatedConstructor.typeParameterSymbols) {
                            typeParameter(
                                typeParamSymbol.name,
                                typeParamSymbol.variance,
                                typeParamSymbol.isReified,
                                Key
                            ) {
                                for (boundRef in typeParamSymbol.resolvedBounds) {
                                    bound(boundRef.type)
                                }
                            }
                        }
                    }.symbol
                } else {
                    null
                }
            }
        }
    }

    /*
    * Generates constructors for:
    * - companion object
    * - ConstructorArgumentsKey base class
    * - ConstructorArgumentsKey descendants
    * */
    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val origin = context.owner.origin as? FirDeclarationOrigin.Plugin
        return if (origin?.key == Key) {
            val sourceDataClassClassId = context
                .owner.classId // ConstructorArgumentsKey descendent
                .parentClassId // ConstructorArgumentsKey base class
                ?.parentClassId // companion
                ?.parentClassId // data class
            val associatedConstructor = session.cachingFactoryInfoProvider
                .getAssociatedConstructors(sourceDataClassClassId)
                ?.get(context.owner.name)
            val constructor = if (associatedConstructor != null) {
                createConstructor(context.owner, Key, isPrimary = true, generateDelegatedNoArgConstructorCall = true) {
                    for (ctorValueParam in associatedConstructor.valueParameterSymbols) {
                        // TODO: default initializer
                        valueParameter(
                            ctorValueParam.name,
                            ctorValueParam.resolvedReturnType,
                            isVararg = ctorValueParam.isVararg,
                            hasDefaultValue = ctorValueParam.hasDefaultValue
                        )
                    }
                }
            } else {
                createDefaultPrivateConstructor(context.owner, Key)
            }
            listOf(constructor.symbol)
        } else {
            emptyList()
        }
    }

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirNamedFunctionSymbol> {
        require(context != null)
        return when(callableId.callableName) {
            Names.FACTORY_METHOD -> {
                val dataClassClassId = context.owner.classId.parentClassId!!
                val dataClass = session.symbolProvider
                    .getClassLikeSymbolByClassId(dataClassClassId)!!
                val constructors = session.cachingFactoryInfoProvider.getAssociatedConstructors(dataClassClassId)!!
                val returnType = dataClassClassId.defaultType(dataClass.typeParameterSymbols)
                constructors.entries.map { (constructorKey, constructorSymbol) ->
                    createMemberFunction(context.owner, Key, Names.FACTORY_METHOD, returnType) {
                        for (ctorTypeParam in constructorSymbol.typeParameterSymbols) {
                            typeParameter(
                                ctorTypeParam.name,
                                ctorTypeParam.variance,
                                ctorTypeParam.isReified
                            ) {
                                if (ctorTypeParam.isBound) {
                                    for (typeParamBound in ctorTypeParam.resolvedBounds) {
                                        bound(typeParamBound.type)
                                    }
                                }
                            }
                        }

                        for (ctorValueParam in constructorSymbol.valueParameterSymbols) {
                            // TODO: default initializer
                            valueParameter(
                                ctorValueParam.name,
                                ctorValueParam.resolvedReturnType,
                                hasDefaultValue = ctorValueParam.hasDefaultValue
                            )
                        }
                    }.symbol
                }
            }
            else -> emptyList()
        }
    }

    // currently only cache property inside companion object
    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirPropertySymbol> {
        require(context != null)

        // TODO: data class constructor fields
        val sourceDataClassClassId = context
            .owner.classId // ConstructorArgumentsKey descendent
            .parentClassId // ConstructorArgumentsKey base class
            ?.parentClassId // companion
            ?.parentClassId // data class
        val associatedSourceConstructor = session.cachingFactoryInfoProvider
            .getAssociatedConstructors(sourceDataClassClassId)
            ?.get(context.owner.name)
        if (associatedSourceConstructor != null) {
            val sourceValueParam = associatedSourceConstructor
                .valueParameterSymbols
                .first { it.name == callableId.callableName }
            val property = createMemberProperty(
                owner = context.owner,
                key = Key,
                name = callableId.callableName,
                returnType = sourceValueParam.resolvedReturnType,
                hasBackingField = true,
                isVal = true
            )
            return listOf(property.symbol)
        }

        if (callableId.callableName != Names.OBJECT_CACHE) return emptyList()

        val companionObject = context.owner
        val companionObjectClassId = context.owner.classId

        val mapType = Names.MAP_CLASS_ID.toConeType(
            arrayOf(
                ClassId(companionObjectClassId.asSingleFqName(), Names.CONSTRUCTOR_ARGUMENTS_BASE_CLASS).toConeType(),
                companionObjectClassId.parentClassId!!.toConeType()
            )
        )

        val cacheProp = createMemberProperty(
            owner = companionObject,
            key = Key,
            name = callableId.callableName,
            returnType = mapType,
            hasBackingField = true,
            isVal = true
        ) {
            visibility = Visibilities.Private
        }

        return listOf(cacheProp.symbol)
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        if (classSymbol !is FirRegularClassSymbol) return emptySet()
        val classId = classSymbol.classId
        if (!classId.isNestedClass) return emptySet()

        val parentClassId = classSymbol.classId.parentClassId ?: return emptySet()
        val parentClassSymbol = session.symbolProvider
            .getClassLikeSymbolByClassId(parentClassId) ?: return emptySet()

        val origin = classSymbol.origin as? FirDeclarationOrigin.Plugin

        return when (classSymbol.classKind) {
            ClassKind.OBJECT -> {
                if (classSymbol.isCompanion && parentClassSymbol.isData) {
                    val names = buildSet {
                        if (origin?.key == Key) {
                            add(SpecialNames.INIT)
                        }

                        // cache: MutableMap<ConstructorArgumentsKey, SomeTransformed<*>>
                        add(Names.OBJECT_CACHE)
                        // "create(...)" methods
                        add(Names.FACTORY_METHOD)
                    }
                    return names
                } else {
                    emptySet()
                }
            }

            ClassKind.CLASS -> {
                if (origin?.key == Key) {
                    buildSet {
                        add(SpecialNames.INIT)
                        val sourceDataClassClassId = parentClassId.parentClassId?.parentClassId
                        val associatedConstructor = session.cachingFactoryInfoProvider
                            .getAssociatedConstructors(sourceDataClassClassId)?.get(classSymbol.name)
                        if (associatedConstructor != null) {
                            // properties for generated ConstructorArgumentsKey
                            for (valueParameter in associatedConstructor.valueParameterSymbols) {
                                add(valueParameter.name)
                            }
                        }
                    }
                } else {
                    emptySet()
                }
            }

            else -> emptySet()
        }
    }

    override fun getNestedClassifiersNames(
        classSymbol: FirClassSymbol<*>,
        context: NestedClassGenerationContext
    ): Set<Name> {
        if (classSymbol.isData && session.predicateBasedProvider.matches(
                CACHING_FACTORY_ANNOTATED_PREDICATE,
                classSymbol
            )
        ) {
            return setOf(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
        }

        val parentClassId = classSymbol.classId.parentClassId ?: return emptySet()
        val parentClassSymbol = session.symbolProvider
            .getClassLikeSymbolByClassId(parentClassId) as? FirRegularClassSymbol ?: return emptySet()

        if (parentClassSymbol.isCompanion && classSymbol.classId.shortClassName == Names.CONSTRUCTOR_ARGUMENTS_BASE_CLASS) {
            val dataClassClassId = parentClassId.parentClassId ?: return emptySet()
            val dataClassClassSymbol = session.symbolProvider
                .getClassLikeSymbolByClassId(dataClassClassId) as? FirRegularClassSymbol ?: return emptySet()

            return if (dataClassClassSymbol.isData && session.predicateBasedProvider.matches(
                    CACHING_FACTORY_ANNOTATED_PREDICATE,
                    dataClassClassSymbol
                )
            ) {
                // constructor(x: T) -> private data class DataClassName_constructor_HASH<T>(val x: T): ConstructorArgumentsKey
                return session.cachingFactoryInfoProvider.getAssociatedConstructors(dataClassClassId)?.keys ?: emptySet()
            } else {
                emptySet()
            }
        }

        return if (classSymbol.isCompanion && parentClassSymbol.isData && session.predicateBasedProvider.matches(
                CACHING_FACTORY_ANNOTATED_PREDICATE,
                parentClassSymbol
            )
        ) {
            // private sealed class ConstructorArgumentsKey
            setOf(Names.CONSTRUCTOR_ARGUMENTS_BASE_CLASS)
        } else {
            emptySet()
        }
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(CACHING_FACTORY_ANNOTATED_PREDICATE)
    }

    object Key : GeneratedDeclarationKey() {
        override fun toString(): String {
            return "CachingFactoryGeneratorKey"
        }
    }
}
