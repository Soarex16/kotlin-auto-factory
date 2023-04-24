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
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

class CachingFactoryGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        const val CACHING_FACTORY_NAMES_PREFIX = "__CachingFactoryGenerated__"
        private val CONSTRUCTOR_ARGUMENTS_BASE_CLASS_NAME =
            Name.identifier("${CACHING_FACTORY_NAMES_PREFIX}ConstructorArgumentsKey")

        // TODO: check ability to use Name.special("<cache>")
        private val CACHE_NAME = Name.identifier("${CACHING_FACTORY_NAMES_PREFIX}cache")
        private val FACTORY_METHOD_NAME = Name.identifier("create")
        private val MAP_CLASS_ID = ClassId.fromString("kotlin.collections.MutableMap")
    }

    // TODO: replcae toConeType with defaultType where needed

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext
    ): FirClassLikeSymbol<*>? {
        @Suppress("NAME_SHADOWING")
        val owner = session.symbolProvider
            .getClassLikeSymbolByClassId(owner.classId) as? FirRegularClassSymbol ?: return null
        return when (name) {
            CONSTRUCTOR_ARGUMENTS_BASE_CLASS_NAME -> {
                val costructorArgsBaseClass =
                    createNestedClass(owner, CONSTRUCTOR_ARGUMENTS_BASE_CLASS_NAME, Key, ClassKind.CLASS) {
                        modality = Modality.SEALED
                        visibility = Visibilities.Public // TODO: private
                    }
                costructorArgsBaseClass.symbol
            }

            SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT -> {
                if (owner.companionObjectSymbol == null) {
                    createCompanionObject(owner, Key).symbol
                } else {
                    null
                }
            }

            else -> {
                if (owner.classId.shortClassName == CONSTRUCTOR_ARGUMENTS_BASE_CLASS_NAME) {
                    val sourceDataClassId = owner.classId.parentClassId!!
                    // TODO: data class fields
                    // val correspondingConstructor = session.cachingFactoryInfoProvider.constructorsToTransform(sourceDataClassId)!![name]!!// annotatedClassesConstructors[sourceDataClassId]!![name]!!
                    createNestedClass(owner, name, Key, ClassKind.CLASS) {
                        modality = Modality.SEALED
                        visibility = Visibilities.Public // TODO: private
                        status {
                            isData = true
                        }
                        superType(owner.classId.toConeType())
                    }.symbol
                } else {
                    null
                }
            }
        }
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val origin = context.owner.origin as? FirDeclarationOrigin.Plugin
        return if (origin?.key == Key) {
            val constructor = createDefaultPrivateConstructor(context.owner, Key)
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
            FACTORY_METHOD_NAME -> {
                val dataClassClassId = context.owner.classId.parentClassId!!
                val constructors = session.cachingFactoryInfoProvider.constructorsToTransform(dataClassClassId)!!
                val returnType = dataClassClassId.toConeType()
                constructors.entries.map { (constructorKey, constructorSymbol) ->
                    createMemberFunction(context.owner, Key, FACTORY_METHOD_NAME, returnType) {
                        for (ctorTypeParam in constructorSymbol.typeParameterSymbols) {
                            // TODO: default initializer
                            typeParameter(
                                ctorTypeParam.name,
                                ctorTypeParam.variance,
                                ctorTypeParam.isReified
                            ) {
                                // TODO: bounds
                                /*if (ctorTypeParam.isBound) {
                                    for (typeParamBound in ctorTypeParam.bounds) {
                                        bound(typeParamBound.type)
                                    }
                                }*/
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

    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirPropertySymbol> {
        require(context != null)
        if (callableId.callableName != CACHE_NAME) return emptyList()

        val companionObject = context.owner
        val companionObjectClassId = context.owner.classId

        val mapType = MAP_CLASS_ID.toConeType(
            arrayOf( // TODO: type params
                ClassId(companionObjectClassId.asSingleFqName(), CONSTRUCTOR_ARGUMENTS_BASE_CLASS_NAME).toConeType(),
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
        )

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
                    // cache: MutableMap<CtorArgs, SomeTransformed<*>>
                    // "create(...)" methods

                    val names = buildSet {
                        if (origin?.key == Key) {
                            add(SpecialNames.INIT)
                        }

                        add(CACHE_NAME)
                        add(FACTORY_METHOD_NAME)
                    }
                    return names
                } else {
                    emptySet()
                }
            }

            ClassKind.CLASS -> {
                if (origin?.key == Key) {
                    setOf(SpecialNames.INIT)
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

        if (parentClassSymbol.isCompanion && classSymbol.classId.shortClassName == CONSTRUCTOR_ARGUMENTS_BASE_CLASS_NAME) {
            val dataClassClassId = parentClassId.parentClassId ?: return emptySet()
            val dataClassClassSymbol = session.symbolProvider
                .getClassLikeSymbolByClassId(dataClassClassId) as? FirRegularClassSymbol ?: return emptySet()

            return if (dataClassClassSymbol.isData && session.predicateBasedProvider.matches(
                    CACHING_FACTORY_ANNOTATED_PREDICATE,
                    dataClassClassSymbol
                )
            ) {
                // private sealed class ConstructorArgumentsKey
                // constructor(x: T) -> private data class DataClassName_hash<T>(val x: T): ConstructorArgumentsKey
                return session.cachingFactoryInfoProvider.constructorsToTransform(dataClassClassId)?.keys ?: emptySet()
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
            setOf(CONSTRUCTOR_ARGUMENTS_BASE_CLASS_NAME)
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
