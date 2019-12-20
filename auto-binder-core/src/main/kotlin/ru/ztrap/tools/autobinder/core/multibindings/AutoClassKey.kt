package ru.ztrap.tools.autobinder.core.multibindings

import dagger.MapKey
import kotlin.reflect.KClass

/** Representation of [dagger.multibindings.ClassKey] for `AutoBindTo` applicable to class. */
@MapKey
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoClassKey(val value: KClass<*>)