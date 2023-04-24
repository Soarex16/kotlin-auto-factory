package com.soarex.autofactory.kotlin.plugin.fir

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.extensions.transform
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol

/**
 *
 */
class CachingFactoryConstructorStatusTransformer(session: FirSession) : FirStatusTransformerExtension(session) {
    override fun transformStatus(
        status: FirDeclarationStatus,
        constructor: FirConstructor,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirDeclarationStatus {
        session.cachingFactoryInfoProvider.markConstructor(constructor)
        return status.transform(visibility = Visibilities.Private)
    }

    override fun needTransformStatus(declaration: FirDeclaration): Boolean {
        if (declaration !is FirConstructor) return false
        val classLikeSymbol = declaration.returnTypeRef.coneType.toRegularClassSymbol(session)!!
        return declaration.status.visibility != Visibilities.Private
                && classLikeSymbol.resolvedStatus.isData
                && session.predicateBasedProvider.matches(CACHING_FACTORY_ANNOTATED_PREDICATE, classLikeSymbol)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(CACHING_FACTORY_ANNOTATED_PREDICATE)
    }
}
