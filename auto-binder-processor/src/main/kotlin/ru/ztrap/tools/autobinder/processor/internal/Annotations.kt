package ru.ztrap.tools.autobinder.processor.internal

import com.squareup.javapoet.ClassName
import ru.ztrap.tools.autobinder.core.Unscoped

internal val NAMED = ClassName.get("javax.inject", "Named")
internal val MODULE = ClassName.get("dagger", "Module")
internal val BINDS = ClassName.get("dagger", "Binds")
internal val UNSCOPED = Unscoped::class.toClassName()