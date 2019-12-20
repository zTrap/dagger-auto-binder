package ru.ztrap.tools.autobinder.core

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoBindTo(
    val value: KClass<*>,
    val type: Type = Type.DEFAULT,
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
