package ru.ztrap.tools.autobinder.core

import kotlin.reflect.KClass

/**
 * @author pa.gulko zTrap (30.10.2019)
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoBindTo(
    val value: KClass<*>,
    val scopeName: String = "",
    val scopeQualifier: KClass<out Annotation> = Unscoped::class
)