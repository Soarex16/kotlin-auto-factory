package com.soarex.autofactory.kotlin.plugin.fir

import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object Names {
    private const val CACHING_FACTORY_NAMES_PREFIX = "__CachingFactoryGenerated__"
    val CONSTRUCTOR_ARGUMENTS_BASE_CLASS =
        Name.identifier("${CACHING_FACTORY_NAMES_PREFIX}ConstructorArgumentsKey")

    val OBJECT_CACHE = Name.identifier("${CACHING_FACTORY_NAMES_PREFIX}cache")
    val FACTORY_METHOD = Name.identifier("create")
    val MUTABLE_MAP_OF_CALLABLE_ID = CallableId(FqName("kotlin.collections"), null, Name.identifier("mutableMapOf"))
    val GET_OR_PUT_CALLABLE_ID = CallableId(FqName("kotlin.collections"), null, Name.identifier("getOrPut"))
    val MUTABLE_MAP_FQN = FqName("kotlin.collections.MutableMap")

    fun createNameForConstructorCacheKey(ctor: FirConstructorSymbol): Name {
        val valueParameterTypesSignatureString =
            ctor.valueParameterSymbols.map { it.resolvedReturnType.classId }.joinToString("_")
        val signatureHash = Int.MAX_VALUE.toLong() + 1 + valueParameterTypesSignatureString.hashCode()
        return Name.identifier("${ctor.name}_constructor_${signatureHash}")
    }
}

val CachingFactoryClassId = ClassId(FqName("com.soarex.autofactory.annotations"), Name.identifier("CachingFactory"))
val IgnoreInCachingFactoryClassId = ClassId(FqName("com.soarex.autofactory.annotations"), Name.identifier("IgnoreInCachingFactory"))

val CACHING_FACTORY_ANNOTATED_PREDICATE = LookupPredicate.create { annotated(CachingFactoryClassId.asSingleFqName()) }
val IGNORE_IN_CACHING_FACTORY_ANNOTATED_PREDICATE = LookupPredicate.create { annotated(IgnoreInCachingFactoryClassId.asSingleFqName()) }

fun ClassId.toConeType(typeArguments: Array<ConeTypeProjection> = emptyArray()): ConeClassLikeType {
    val lookupTag = ConeClassLikeLookupTagImpl(this)
    return ConeClassLikeTypeImpl(lookupTag, typeArguments, isNullable = false)
}