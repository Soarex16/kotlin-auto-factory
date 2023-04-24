package com.soarex.autofactory.kotlin.plugin.fir

import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

val CachingFactoryClassId = ClassId(FqName("com.soarex.autofactory.annotation"), Name.identifier("CachingFactory"))

val CACHING_FACTORY_ANNOTATED_PREDICATE = LookupPredicate.create { annotated(CachingFactoryClassId.asSingleFqName()) }

//val CACHING_FACTORY_COMPANION_PREDICATE = LookupPredicate.create { ancestorAnnotated(CachingFactoryClassId.asSingleFqName()) }

fun ClassId.toConeType(typeArguments: Array<ConeTypeProjection> = emptyArray()): ConeClassLikeType {
    val lookupTag = ConeClassLikeLookupTagImpl(this)
    return ConeClassLikeTypeImpl(lookupTag, typeArguments, isNullable = false)
}