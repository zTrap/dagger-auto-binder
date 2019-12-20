package ru.ztrap.tools.autobinder.processor.internal

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

internal fun TypeElement.toClassName(): ClassName = ClassName.get(this)
internal fun TypeMirror.toTypeName(): TypeName = TypeName.get(this)
internal fun KClass<*>.toClassName(): ClassName = ClassName.get(java)

internal fun ClassName.canonicalName(): String {
    return buildString {
        append(packageName())
        append(".")
        append(simpleNames().joinToString(separator = "."))
    }
}

// TODO https://github.com/square/javapoet/issues/671
internal fun TypeName.toClassName(): ClassName = when (this) {
    is ClassName -> this
    is ParameterizedTypeName -> rawType
    else -> throw IllegalStateException("Cannot extract class name from $this")
}

internal val ClassName.joinedSimpleNames: String
    get() = simpleNames().joinToString(separator = "$")

