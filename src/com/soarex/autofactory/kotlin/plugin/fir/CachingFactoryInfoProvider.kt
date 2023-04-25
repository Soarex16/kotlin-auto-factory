package com.soarex.autofactory.kotlin.plugin.fir

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class CachingFactoryInfoProvider(session: FirSession) : FirExtensionSessionComponent(session) {
    /**
     * Contains mapping from data class classId to another mapping which represents mapping from
     * mangled constructor key class name -> associated constructor symbol
     */
    private val factoryCacheKeysToAssociatedSourceConstructors: Map<ClassId, Map<Name, FirConstructorSymbol>> by session.firCachesFactory.createLazyValue {
        session.predicateBasedProvider
            .getSymbolsByPredicate(CACHING_FACTORY_ANNOTATED_PREDICATE)
            .filterIsInstance<FirRegularClassSymbol>()
            .associateBy({ it.classId }) { classSymbol ->
                val transformedConstructors = transformedConstructorsCache.getValue(classSymbol.classId)
                val ignoredConstructors = explicitlyIgnoredConstructors[classSymbol.classId]
                classSymbol.declarationSymbols
                    .filterIsInstance<FirConstructorSymbol>()
                    .filter { ctor ->
                        (ctor.rawStatus.visibility != Visibilities.Private || ctor in transformedConstructors) && (ignoredConstructors == null || ctor !in ignoredConstructors)
                    }
                    .associateBy { Names.createNameForConstructorCacheKey(it) }
            }
    }

    fun getAssociatedConstructors(classId: ClassId?): Map<Name, FirConstructorSymbol>? {
        return factoryCacheKeysToAssociatedSourceConstructors[classId]
    }

    private val explicitlyIgnoredConstructors: Map<ClassId, Set<FirConstructorSymbol>> by session.firCachesFactory.createLazyValue {
        session.predicateBasedProvider
            .getSymbolsByPredicate(IGNORE_IN_CACHING_FACTORY_ANNOTATED_PREDICATE)
            .filterIsInstance<FirConstructorSymbol>()
            .groupingBy { it.resolvedReturnType.classId!! }
            .fold(
                { _, _ -> mutableSetOf() },
                { _, symbols, ctor -> symbols.apply { add(ctor) } }
            )
    }

    private val transformedConstructorsCache: FirCache<ClassId, MutableSet<FirConstructorSymbol>, Nothing?> =
        session.firCachesFactory.createCache { _ -> mutableSetOf() }

    fun markConstructorAsTransformed(constructorSymbol: FirConstructorSymbol) {
        val classId = constructorSymbol.resolvedReturnType.classId!!
        transformedConstructorsCache.getValue(classId).add(constructorSymbol)
    }

    fun isConstructorMarkedAsTransformed(constructorSymbol: FirConstructorSymbol): Boolean {
        val classId = constructorSymbol.resolvedReturnType.classId!!
        return constructorSymbol in transformedConstructorsCache.getValue(classId)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(CACHING_FACTORY_ANNOTATED_PREDICATE)
        register(IGNORE_IN_CACHING_FACTORY_ANNOTATED_PREDICATE)
    }
}

val FirSession.cachingFactoryInfoProvider: CachingFactoryInfoProvider by FirSession.sessionComponentAccessor()