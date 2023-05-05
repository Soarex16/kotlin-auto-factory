package com.soarex.autofactory.kotlin.plugin.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.expressions.buildResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.plugin.*
import org.jetbrains.kotlin.fir.references.builder.buildPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.types.Variance

/**
 * Note: we don't support default values for constructor value params
 * Because in this case we need some kind of value parameter remapper,
 * because AST for default value expression can reference other value parameters
 */
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
                createNestedClass(
                    owner,
                    Names.CONSTRUCTOR_ARGUMENTS_BASE_CLASS,
                    Key.ConstructorKeyBaseClass,
                    ClassKind.CLASS
                ) {
                    modality = Modality.SEALED
                    visibility = Visibilities.Private
                }.symbol
            }

            SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT -> {
                if (owner.companionObjectSymbol == null) {
                    createCompanionObject(owner, Key.Companion).symbol
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
                    createNestedClass(owner, name, Key.ConstructorKey, ClassKind.CLASS) {
                        modality = Modality.FINAL
                        status {
                            isData = true
                        }
                        superType(owner.classId.defaultType(owner.typeParameterSymbols))

                        for (typeParamSymbol in associatedConstructor.typeParameterSymbols) {
                            typeParameter(
                                typeParamSymbol.name,
                                typeParamSymbol.variance,
                                typeParamSymbol.isReified,
                                Key.Other
                            ) {
                                for (boundRef in typeParamSymbol.resolvedBounds) {
                                    bound(boundRef.type)
                                }
                            }
                        }
                    }.also {
                        it.remapTypeParameters(associatedConstructor.typeParameterSymbols)
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
    * - ConstructorArgumentsKey subclasses
    * */
    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val origin = context.owner.origin as? FirDeclarationOrigin.Plugin
        return if (origin?.key is Key) {
            val sourceDataClassClassId = context
                .owner.classId // ConstructorArgumentsKey subclass
                .parentClassId // ConstructorArgumentsKey base class
                ?.parentClassId // companion
                ?.parentClassId // data class
            val associatedConstructor = session.cachingFactoryInfoProvider
                .getAssociatedConstructors(sourceDataClassClassId)
                ?.get(context.owner.name)
            val constructor = if (associatedConstructor != null) {
                createConstructor(
                    context.owner,
                    Key.Other,
                    isPrimary = true,
                    generateDelegatedNoArgConstructorCall = true
                ) {
                    for (ctorValueParam in associatedConstructor.valueParameterSymbols) {
                        valueParameter(
                            ctorValueParam.name,
                            ctorValueParam.resolvedReturnType,
                            isVararg = ctorValueParam.isVararg
                        )
                    }
                }
            } else {
                createConstructor(
                    context.owner,
                    Key.Other,
                    isPrimary = true,
                    generateDelegatedNoArgConstructorCall = true
                ) {
                    visibility = if (context.owner.name == Names.CONSTRUCTOR_ARGUMENTS_BASE_CLASS) {
                        Visibilities.Protected
                    } else {
                        Visibilities.Private
                    }
                }
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
                    val constructorKeyClassId =
                        context.owner.classId.createNestedClassId(Names.CONSTRUCTOR_ARGUMENTS_BASE_CLASS)
                            .createNestedClassId(constructorKey)
                    val declarationKey = Key.CreateFunction(constructorKeyClassId)

                    createMemberFunction(context.owner, declarationKey, Names.FACTORY_METHOD, returnType) {
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
                            valueParameter(
                                ctorValueParam.name,
                                ctorValueParam.resolvedReturnType
                            )
                        }
                    }.also {
                        it.remapTypeParameters(constructorSymbol.typeParameterSymbols)
                    }.symbol
                }
            }

            else -> emptyList()
        }
    }

    @OptIn(SymbolInternals::class)
    private fun FirFunction.remapTypeParameters(oldTypeParameters: List<FirTypeParameterSymbol>) {
        val substitution = oldTypeParameters
            .zip(this.typeParameters)
            .associate { (oldTp, newTp) -> oldTp to newTp.toConeType() }
        val substitutor = ConeSubstitutorByMap(substitution, session)

        for (typeParameter in this.typeParameters) {
            val newBounds = typeParameter.symbol.resolvedBounds
                .map { substitutor.substituteOrSelf(it.coneType).toFirResolvedTypeRef() }
            typeParameter.symbol.fir.replaceBounds(newBounds)
        }

        this.transformValueParameters(object : FirTransformer<Nothing?>() {
            override fun <E : FirElement> transformElement(element: E, data: Nothing?): E =
                if (element is FirValueParameter) {
                    val type = substitutor.substituteOrSelf(element.returnTypeRef.coneType)
                    element.replaceReturnTypeRef(type.toFirResolvedTypeRef())
                    element
                } else {
                    element
                }
        }, null)
    }

    @OptIn(SymbolInternals::class)
    private fun FirClass.remapTypeParameters(oldTypeParameters: List<FirTypeParameterSymbol>) {
        val substitution = oldTypeParameters
            .zip(this.typeParameters)
            .associate { (oldTp, newTp) -> oldTp to newTp.toConeType() }
        val substitutor = ConeSubstitutorByMap(substitution, session)

        for (typeParameter in this.typeParameters) {
            val newBounds = typeParameter.symbol.resolvedBounds
                .map { substitutor.substituteOrSelf(it.coneType).toFirResolvedTypeRef() }
            typeParameter.symbol.fir.replaceBounds(newBounds)
        }
    }

    @OptIn(SymbolInternals::class)
    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirPropertySymbol> {
        require(context != null)

        // properties for data class associated with constructor
        val sourceDataClassClassId = context
            .owner.classId // ConstructorArgumentsKey subclass
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
                key = Key.Other,
                name = callableId.callableName,
                returnType = sourceValueParam.resolvedReturnType,
                hasBackingField = true,
                isVal = true
            ).also {
                val primaryConstructorParameter = context.owner.fir
                    .primaryConstructorIfAny(session)!!
                    .valueParameterSymbols
                    .first { vp -> vp.name == callableId.callableName }
                it.replaceInitializer(buildPropertyAccessExpression {
                    calleeReference = buildPropertyFromParameterResolvedNamedReference {
                        name = callableId.callableName
                        resolvedSymbol = primaryConstructorParameter
                    }
                })
            }
            return listOf(property.symbol)
        }

        if (callableId.callableName != Names.OBJECT_CACHE) return emptyList()

        val companionObject = context.owner
        val companionObjectClassId = context.owner.classId

        val typeArgs: Array<ConeTypeProjection> = arrayOf(
            ClassId(companionObjectClassId.asSingleFqName(), Names.CONSTRUCTOR_ARGUMENTS_BASE_CLASS).toConeType(),
            companionObjectClassId.parentClassId!!.toConeType()
        )

        // cache property inside companion object
        val cacheProp = createMemberProperty(
            owner = companionObject,
            key = Key.CacheProperty,
            name = callableId.callableName,
            returnType = StandardClassIds.MutableMap.toConeType(typeArgs),
            hasBackingField = true,
            isVal = true
        ) {
            visibility = Visibilities.Private
        }.also {
            val mutableMapOfCall = buildFunctionCall {
                val mutableMapOfFunction = session.symbolProvider
                    .getTopLevelCallableSymbols(
                        Names.MUTABLE_MAP_OF_CALLABLE_ID.packageName,
                        Names.MUTABLE_MAP_OF_CALLABLE_ID.callableName
                    )
                    .first()
                calleeReference = buildResolvedNamedReference {
                    name = Names.MUTABLE_MAP_OF_CALLABLE_ID.callableName
                    resolvedSymbol = mutableMapOfFunction
                }
                argumentList = buildResolvedArgumentList(LinkedHashMap())
                typeArguments.addAll(
                    typeArgs.map {
                        buildTypeProjectionWithVariance {
                            typeRef = buildResolvedTypeRef { type = it.type!! }
                            variance = Variance.INVARIANT
                        }
                    })
            }

            it.replaceInitializer(mutableMapOfCall)
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
                if (classSymbol.isCompanion && parentClassSymbol.isData && session.predicateBasedProvider.matches(
                        CACHING_FACTORY_ANNOTATED_PREDICATE,
                        parentClassSymbol
                    )
                ) {
                    val names = buildSet {
                        if (origin?.key is Key) {
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
                if (origin?.key is Key) {
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

    sealed class Key : GeneratedDeclarationKey() {
        data class CreateFunction(val associatedConstructorKeyType: ClassId) : Key()
        data object ConstructorKey : Key()
        data object ConstructorKeyBaseClass : Key()
        data object CacheProperty : Key()
        data object Companion : Key()
        data object Other : Key()
    }
}
