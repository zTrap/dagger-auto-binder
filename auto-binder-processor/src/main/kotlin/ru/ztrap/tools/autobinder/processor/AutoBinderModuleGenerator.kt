package ru.ztrap.tools.autobinder.processor

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import ru.ztrap.tools.autobinder.core.AutoBindTo
import ru.ztrap.tools.autobinder.processor.internal.BINDS
import ru.ztrap.tools.autobinder.processor.internal.MODULE
import ru.ztrap.tools.autobinder.processor.internal.NAMED
import ru.ztrap.tools.autobinder.processor.internal.UNSCOPED
import ru.ztrap.tools.autobinder.processor.internal.applyEach
import ru.ztrap.tools.autobinder.processor.internal.castEach
import ru.ztrap.tools.autobinder.processor.internal.findElementsAnnotatedWith
import ru.ztrap.tools.autobinder.processor.internal.joinedSimpleNames
import ru.ztrap.tools.autobinder.processor.internal.toClassName
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.Modifier.PRIVATE
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.MirroredTypeException

class AutoBinderModuleGenerator(
    private val public: Boolean,
    moduleName: ClassName,
    roundEnvironment: RoundEnvironment
) {
    private val bindElements = roundEnvironment.findAutoBinderElementsOrNull()

    val generatedType = moduleName.autoBinderModuleName()

    fun brewJava(): TypeSpec {
        return TypeSpec.classBuilder(generatedType)
            .addAnnotation(MODULE)
            .addModifiers(ABSTRACT)
            .apply {
                if (public) {
                    addModifiers(PUBLIC)
                }
            }
            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(PRIVATE)
                    .build()
            )
            .applyEach(bindElements) { element ->
                addMethod(element.brewJava())
            }
            .build()
    }

    private fun RoundEnvironment.findAutoBinderElementsOrNull(): List<BindElement> {
        return findElementsAnnotatedWith<AutoBindTo>()
            .castEach<TypeElement>()
            .map(::BindElement)
    }
}

private class BindElement(annotatedType: TypeElement) {
    val targetType: ClassName
    val bindType: ClassName
    val scopeName: String
    val scopeQualifier: ClassName

    init {
        val annotation = annotatedType.getAnnotation(AutoBindTo::class.java)
        targetType = annotatedType.toClassName()
        bindType = getClassName { annotation.value.toClassName() }
        scopeName = annotation.scopeName
        scopeQualifier = getClassName { annotation.scopeQualifier.toClassName() }
    }

    private fun getClassName(extractor: () -> ClassName): ClassName {
        return try {
            extractor()
        } catch (e: MirroredTypeException) {
            val classTypeMirror = e.typeMirror as DeclaredType
            val classTypeElement = classTypeMirror.asElement() as TypeElement
            classTypeElement.toClassName()
        }
    }

    fun brewJava(): MethodSpec {
        return MethodSpec.methodBuilder(bindMethodName)
            .addAnnotation(BINDS)
            .addModifiers(ABSTRACT)
            .returns(bindType)
            .addParameter(targetType, "binding")
            .also {
                if (scopeName.isNotEmpty()) {
                    val named = AnnotationSpec.builder(NAMED)
                        .addMember("value", "\$S", scopeName)
                        .build()
                    it.addAnnotation(named)
                }
                if (scopeQualifier != UNSCOPED) {
                    val qualifier = AnnotationSpec.builder(scopeQualifier).build()
                    it.addAnnotation(qualifier)
                }
            }
            .build()
    }

    private val bindMethodName: String
        get() = "bind${targetType.joinedSimpleNames}To${bindType.joinedSimpleNames}"
}

fun ClassName.autoBinderModuleName(): ClassName {
    return ClassName.get(packageName(), simpleNames().joinToString("_", prefix = "AutoBinder_"))
}
