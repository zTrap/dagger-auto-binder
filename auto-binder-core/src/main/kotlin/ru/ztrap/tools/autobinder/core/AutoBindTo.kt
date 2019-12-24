package ru.ztrap.tools.autobinder.core

import kotlin.reflect.KClass

/** Identifies a class for creating auto-bind method in generated dagger module for [AutoBinderModule] marker. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoBindTo(
    /** Return type for `@Binds` method */
    val value: KClass<*>,
    /** Type of `@Binds` method */
    val type: Type = Type.DEFAULT,
    /** If this parameter is true - `@Qualifier`-annotation will be copied to the `@Binds` method */
    val translateQualifier: Boolean = true
) {
    enum class Type {
        /** Automatically adds [dagger.multibindings.IntoSet] annotation to generated method. */
        INTO_SET,
        /**
         * Automatically adds [dagger.multibindings.IntoMap] annotation to generated method.
         * Also, you need to annotate class with an annotation, annotated with [dagger.MapKey].
         */
        INTO_MAP,
        /** This one does additionally nothing */
        DEFAULT
    }
}
