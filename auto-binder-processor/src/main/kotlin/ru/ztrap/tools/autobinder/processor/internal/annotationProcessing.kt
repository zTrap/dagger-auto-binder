package ru.ztrap.tools.autobinder.processor.internal

import com.squareup.javapoet.ClassName
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.AnnotatedConstruct
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.ErrorType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.SimpleAnnotationValueVisitor6
import javax.lang.model.util.SimpleTypeVisitor6

/** Return a list of elements annotated with `T`. */
internal inline fun <reified T : Annotation> RoundEnvironment.findElementsAnnotatedWith(): Set<Element>
    = getElementsAnnotatedWith(T::class.java)

/** Return true if this [AnnotatedConstruct] is annotated with [className]. */
internal fun AnnotatedConstruct.hasAnnotation(className: ClassName) = getAnnotation(className) != null

/** Return the first annotation matching [className.canonicalName] or null. */
internal fun AnnotatedConstruct.getAnnotation(className: ClassName) = annotationMirrors
    .firstOrNull {
        it.annotationType
            .asElement()
            .cast<TypeElement>()
            .qualifiedName
            .contentEquals(className.canonicalName())
    }

internal fun AnnotationMirror.getValue(property: String, elements: Elements) = elements
    .getElementValuesWithDefaults(this)
    .entries
    .firstOrNull { it.key.simpleName.contentEquals(property) }
    ?.value
    ?.toMirrorValue()

internal fun AnnotationValue.toMirrorValue(): MirrorValue = accept(
    MirrorValueVisitor, null)

internal sealed class MirrorValue {
  data class Type(private val value: TypeMirror) : MirrorValue(), TypeMirror by value
  data class Array(private val value: List<MirrorValue>) : MirrorValue(), List<MirrorValue> by value
  object Unmapped : MirrorValue()
  object Error : MirrorValue()
}

private object MirrorValueVisitor : SimpleAnnotationValueVisitor6<MirrorValue, Nothing?>() {
  override fun defaultAction(o: Any, ignored: Nothing?) = MirrorValue.Unmapped

  override fun visitType(mirror: TypeMirror, ignored: Nothing?): MirrorValue = mirror.accept(TypeVisitor, null)

  override fun visitArray(values: List<AnnotationValue>, ignored: Nothing?) =
      MirrorValue.Array(values.map { it.accept(this, null) })
}

private object TypeVisitor : SimpleTypeVisitor6<MirrorValue, Nothing?>() {
  override fun visitError(type: ErrorType, ignored: Nothing?) = MirrorValue.Error
  override fun defaultAction(type: TypeMirror, ignored: Nothing?) = MirrorValue.Type(type)
}
