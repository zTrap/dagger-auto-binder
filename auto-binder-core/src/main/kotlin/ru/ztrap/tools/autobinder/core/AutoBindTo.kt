package ru.ztrap.tools.autobinder.core

import kotlin.reflect.KClass

@Suppress("DEPRECATION")
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoBindTo(
    val value: KClass<*>,
    val scopeName: String = "",
    val scopeQualifier: KClass<out Annotation> = Unscoped::class
)