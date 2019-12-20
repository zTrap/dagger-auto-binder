package ru.ztrap.tools.autobinder.processor.internal

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import javax.annotation.processing.Processor
import javax.lang.model.SourceVersion
import javax.lang.model.util.Elements

internal val QUALIFIER = ClassName.get("javax.inject", "Qualifier")
internal val MODULE = ClassName.get("dagger", "Module")
internal val MAP_KEY = ClassName.get("dagger", "MapKey")
internal val BINDS = ClassName.get("dagger", "Binds")
internal val INTO_SET = ClassName.get("dagger.multibindings", "IntoSet")
internal val INTO_MAP = ClassName.get("dagger.multibindings", "IntoMap")


/**
 * Create a `@Generated` annotation using the correct type based on source version and availability
 * on the compilation classpath, a `value` with the fully-qualified class name of the calling
 * [Processor], and a comment pointing to this project's GitHub repo. Returns `null` if no
 * annotation type is available on the classpath.
 */
internal fun Processor.createGeneratedAnnotation(
    sourceVersion: SourceVersion,
    elements: Elements
): AnnotationSpec? {
    val annotationTypeName = when {
        sourceVersion <= SourceVersion.RELEASE_8 -> "javax.annotation.Generated"
        else -> "javax.annotation.processing.Generated"
    }
    val generatedType = elements.getTypeElement(annotationTypeName) ?: return null
    return AnnotationSpec.builder(generatedType.toClassName())
        .addMember("value", "\$S", javaClass.name)
        .addMember("comments", "\$S", "https://github.com/zTrap/Auto-binder")
        .build()
}