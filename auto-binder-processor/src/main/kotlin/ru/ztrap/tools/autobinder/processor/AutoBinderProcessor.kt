package ru.ztrap.tools.autobinder.processor

import com.google.auto.common.MoreElements
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType
import ru.ztrap.tools.autobinder.core.AutoBindTo
import ru.ztrap.tools.autobinder.core.Unscoped
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.MirroredTypeException

@Suppress("DEPRECATION")
@IncrementalAnnotationProcessor(IncrementalAnnotationProcessorType.DYNAMIC)
@AutoService(Processor::class)
class AutoBinderProcessor : AbstractProcessor() {

    companion object {
        private const val MODULE_NAME = "AutoBinderModule"
        private val ANNOTATION_CLASS = AutoBindTo::class.java
        private val UNSCOPED_CLASS_NAME = Unscoped::class.asClassName()
        private val MODULE_ANNOTATION = AnnotationSpec.builder(ClassName("dagger", "Module")).build()
        private val BINDS_ANNOTATION = AnnotationSpec.builder(ClassName("dagger", "Binds")).build()
    }

    private val autoBinderModule = TypeSpec.classBuilder(MODULE_NAME)
        .addModifiers(KModifier.ABSTRACT)
        .addAnnotation(MODULE_ANNOTATION)

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()
    override fun getSupportedAnnotationTypes(): Set<String> = setOf(ANNOTATION_CLASS.canonicalName)

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val elements = roundEnv.getElementsAnnotatedWith(ANNOTATION_CLASS)
            .map { it as TypeElement }
            .map(::BindElement)

        if (elements.isEmpty()) return true

        val packageName = elements.map { it.packageName }.commonPart

        elements.forEach(::writeToModule)

        FileSpec.builder(packageName, MODULE_NAME)
            .addComment("Generated by AutoBinder (https://github.com/zTrap/Auto-binder)")
            .addType(autoBinderModule.build())
            .build()
            .writeTo(processingEnv.filer)
        return true
    }

    private val List<String>.commonPart: String
        get() {
            val shortest = minBy { it.length }
            if (!shortest.isNullOrEmpty()) {
                var commonPart = ""

                shortest.indices.forEach { index ->
                    val symbol = shortest[index]
                    if (all { it[index] == symbol }) {
                        commonPart += symbol
                    }
                }

                if (commonPart != shortest) {
                    commonPart = commonPart.substringBeforeLast(".", "")
                }

                if (commonPart.isNotEmpty()) {
                    return commonPart
                }
            }
            return "inject.generated"
        }

    private fun writeToModule(bindElement: BindElement) {
        autoBinderModule.addFunction(
            FunSpec.builder("bind${bindElement.targetType.simpleName}To${bindElement.returnType.simpleName}")
                .addModifiers(KModifier.ABSTRACT)
                .addAnnotation(BINDS_ANNOTATION)
                .addParameter(ParameterSpec.builder("binding", bindElement.targetType).build())
                .returns(bindElement.returnType)
                .also {
                    if (bindElement.scopeName.isNotEmpty()) {
                        val named = AnnotationSpec.builder(ClassName("javax.inject", "Named"))
                            .addMember("value = %S", bindElement.scopeName)
                            .build()
                        it.addAnnotation(named)
                    } else if (bindElement.scopeQualifier != UNSCOPED_CLASS_NAME) {
                        val qualifier = AnnotationSpec.builder(bindElement.scopeQualifier).build()
                        it.addAnnotation(qualifier)
                    }
                }
                .build()
        )
    }

    private inner class BindElement(annotatedType: TypeElement) {
        val packageName: String
        val targetType: ClassName
        val returnType: ClassName
        val scopeName: String
        val scopeQualifier: ClassName

        init {
            val annotation = annotatedType.getAnnotation(ANNOTATION_CLASS)
            packageName = MoreElements.getPackage(annotatedType).qualifiedName.toString()
            targetType = annotatedType.asClassName()
            returnType = getClassName { annotation.value.asClassName() }
            scopeName = annotation.scopeName
            scopeQualifier = getClassName { annotation.scopeQualifier.asClassName() }
        }

        private fun getClassName(extractor: () -> ClassName): ClassName {
            return try {
                extractor()
            } catch (e: MirroredTypeException) {
                val classTypeMirror = e.typeMirror as DeclaredType
                val classTypeElement = classTypeMirror.asElement() as TypeElement
                classTypeElement.asClassName()
            }
        }
    }
}