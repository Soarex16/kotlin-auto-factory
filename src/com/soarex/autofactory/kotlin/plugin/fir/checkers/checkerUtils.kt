package com.soarex.autofactory.kotlin.plugin.fir.checkers

import com.soarex.autofactory.kotlin.plugin.fir.IgnoreInCachingFactoryClassId
import com.soarex.autofactory.kotlin.plugin.fir.cachingFactoryInfoProvider
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol

fun FirConstructorSymbol.suitableForCachingFactory(session: FirSession): Boolean {
    return (!Visibilities.isPrivate(this.visibility) || session.cachingFactoryInfoProvider.isConstructorMarkedAsTransformed(
        this
    )) && !this.hasAnnotation(IgnoreInCachingFactoryClassId, session)
}