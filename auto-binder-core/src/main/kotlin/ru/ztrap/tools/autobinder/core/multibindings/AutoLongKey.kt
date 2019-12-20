package ru.ztrap.tools.autobinder.core.multibindings

import dagger.MapKey

/** Representation of [dagger.multibindings.LongKey] for `AutoBindTo` applicable to class. */
@MapKey
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoLongKey(val value: Long)