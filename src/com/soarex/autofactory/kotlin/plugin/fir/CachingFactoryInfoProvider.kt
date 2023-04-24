package com.soarex.autofactory.kotlin.plugin.fir

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class CachingFactoryInfoProvider(session: FirSession) : FirExtensionSessionComponent(session) {
    // TODO: учесть, что тут может не быть какого-то класса - это значит, что для него мы фабрику не генерируем
    private val annotatedClassesConstructors: Map<ClassId, Map<Name, FirConstructorSymbol>> by session.firCachesFactory.createLazyValue {
        session.predicateBasedProvider
            .getSymbolsByPredicate(CACHING_FACTORY_ANNOTATED_PREDICATE)
            .filterIsInstance<FirRegularClassSymbol>()
            .associateBy({ it.classId }) { classSymbol ->
                classSymbol.declarationSymbols
                    .filterIsInstance<FirConstructorSymbol>()
                    .filter { ctor ->
                        ctor.rawStatus.visibility != Visibilities.Private || ctor in transformedConstructorsCache.getValue(
                            classSymbol.classId
                        )
                    }
                    .associateBy { ctor -> Name.identifier("${CachingFactoryGenerator.CACHING_FACTORY_NAMES_PREFIX}${ctor.name}_Constructor_${ctor.hashCode()}") }
            }
    }

    private val transformedConstructorsCache: FirCache<ClassId, MutableSet<FirConstructorSymbol>, Nothing?> =
        session.firCachesFactory.createCache { classId ->
            mutableSetOf()
            /*
            session.predicateBasedProvider
                .getSymbolsByPredicate(CACHING_FACTORY_ANNOTATED_PREDICATE)
                .filterIsInstance<FirRegularClassSymbol>()
                .associateBy({ it.classId }) {
                    it.declarationSymbols
                        .filterIsInstance<FirConstructorSymbol>()
    //                    .filter { ctor -> ctor.rawStatus.visibility != Visibilities.Private }
                        .associateBy { ctor -> Name.identifier("${CachingFactoryGenerator.CACHING_FACTORY_NAMES_PREFIX}${ctor.name}_Constructor_${ctor.hashCode()}") }
                }
            SerializationPackages.allPublicPackages.firstNotNullOfOrNull { packageName ->
                session.symbolProvider.getClassLikeSymbolByClassId(ClassId(packageName, name)) as? FirClassSymbol<*>
            } ?: throw IllegalArgumentException("Can't locate cass ${name.identifier}")*/
        }

    fun constructorsToTransform(classId: ClassId): Map<Name, FirConstructorSymbol>? {
        return annotatedClassesConstructors[classId]
    }

    fun markConstructor(constructor: FirConstructor) {
        val classId = constructor.symbol.resolvedReturnType.classId!!
        transformedConstructorsCache.getValue(classId).add(constructor.symbol)
    }
}

val FirSession.cachingFactoryInfoProvider: CachingFactoryInfoProvider by FirSession.sessionComponentAccessor()