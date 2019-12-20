package ru.ztrap.tools.autobinder.core.multibindings

import dagger.MapKey

/** Representation of [dagger.multibindings.StringKey] for `AutoBindTo` applicable to class. */
@MapKey
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoStringKey(val value: String)