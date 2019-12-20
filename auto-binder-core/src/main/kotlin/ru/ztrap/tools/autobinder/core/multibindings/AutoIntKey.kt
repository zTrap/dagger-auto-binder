package ru.ztrap.tools.autobinder.core.multibindings

import dagger.MapKey

/** Representation of [dagger.multibindings.IntKey] for `AutoBindTo` applicable to class. */
@MapKey
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoIntKey(val value: Int)