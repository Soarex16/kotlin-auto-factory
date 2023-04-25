package com.soarex.autofactory.annotations

@Target(AnnotationTarget.CLASS)
annotation class CachingFactory

@Target(AnnotationTarget.CONSTRUCTOR)
annotation class IgnoreInCachingFactory
