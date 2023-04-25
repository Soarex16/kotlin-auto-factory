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
        session.cachingFactoryInfoProvider.markConstructorAsTransformed(constructor.symbol)
        return status.transform(visibility = Visibilities.Private)
    }

    override fun needTransformStatus(declaration: FirDeclaration): Boolean {
        if (declaration !is FirConstructor) return false
        val declaringClass = declaration.returnTypeRef.coneType.toRegularClassSymbol(session)!!
        return declaration.status.visibility != Visibilities.Private
                && declaringClass.resolvedStatus.isData
                && !session.predicateBasedProvider.matches(IGNORE_IN_CACHING_FACTORY_ANNOTATED_PREDICATE, declaration)
                && session.predicateBasedProvider.matches(CACHING_FACTORY_ANNOTATED_PREDICATE, declaringClass)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(CACHING_FACTORY_ANNOTATED_PREDICATE)
        register(IGNORE_IN_CACHING_FACTORY_ANNOTATED_PREDICATE)
    }
}
