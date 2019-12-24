package ru.ztrap.tools.autobinder.core

/**
 * This annotation must be used below any dagger module. Single per gradle module.
 *
 * Compiler uses this annotation as marker where generate our module with `@Binds` methods.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoBinderModule