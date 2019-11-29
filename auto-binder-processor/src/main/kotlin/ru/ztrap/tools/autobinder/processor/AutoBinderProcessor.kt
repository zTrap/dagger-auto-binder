package ru.ztrap.tools.autobinder.processor

import com.google.auto.service.AutoService
import com.squareup.javapoet.JavaFile
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType.AGGREGATING
import ru.ztrap.tools.autobinder.core.AutoBindTo
import ru.ztrap.tools.autobinder.core.AutoBinderModule
import ru.ztrap.tools.autobinder.processor.internal.MODULE
import ru.ztrap.tools.autobinder.processor.internal.MirrorValue
import ru.ztrap.tools.autobinder.processor.internal.applyEach
import ru.ztrap.tools.autobinder.processor.internal.cast
import ru.ztrap.tools.autobinder.processor.internal.castEach
import ru.ztrap.tools.autobinder.processor.internal.findElementsAnnotatedWith
import ru.ztrap.tools.autobinder.processor.internal.getAnnotation
import ru.ztrap.tools.autobinder.processor.internal.getValue
import ru.ztrap.tools.autobinder.processor.internal.hasAnnotation
import ru.ztrap.tools.autobinder.processor.internal.toClassName
import ru.ztrap.tools.autobinder.processor.internal.toTypeName
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.tools.Diagnostic

@IncrementalAnnotationProcessor(AGGREGATING)
@AutoService(Processor::class)
class AutoBinderProcessor : AbstractProcessor() {

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    override fun getSupportedAnnotationTypes() = setOf(
        AutoBinderModule::class.java.canonicalName,
        AutoBindTo::class.java.canonicalName
    )

    override fun init(env: ProcessingEnvironment) {
        super.init(env)
        messager = env.messager
        filer = env.filer
        elements = env.elementUtils
    }

    private lateinit var messager: Messager
    private lateinit var filer: Filer
    private lateinit var elements: Elements

    private val unprocessedBindNames = mutableListOf<Name>()
    private var userModule: String? = null

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        // Record factories as fully qualified names so they can safely be accessed in future
        // processing rounds.
        unprocessedBindNames += roundEnv.findElementsAnnotatedWith<AutoBindTo>()
            .castEach<TypeElement>()
            .map { it.qualifiedName }

        val autoBinderModuleElements = roundEnv.findAutoBinderModuleElementsOrNull()
        if (autoBinderModuleElements != null) {
            val moduleType = autoBinderModuleElements.moduleType

            val userModuleFqcn = userModule
            if (userModuleFqcn != null) {
                val userModuleType = elements.getTypeElement(userModuleFqcn)
                error("Multiple @AutoBinderModule-annotated modules found.", userModuleType)
                error("Multiple @AutoBinderModule-annotated modules found.", moduleType)
                userModule = null
            } else {
                userModule = moduleType.qualifiedName.toString()

                val autoBinderModuleGenerator = autoBinderModuleElements.toAutoBinderModuleGenerator(roundEnv)
                writeAutoBinderModule(autoBinderModuleElements, autoBinderModuleGenerator)
            }
        }

        // Wait until processing is ending to validate that the @AutoBinderModule's @Module annotation
        // includes the generated type.
        if (roundEnv.processingOver()) {
            val userModuleFqcn = userModule
            if (userModuleFqcn != null) {
                // In the processing round in which we handle the @AutoBinderModule the @Module annotation's
                // includes contain an <error> type because we haven't generated the AutoBinder module yet.
                // As a result, we need to re-lookup the element so that its referenced types are available.
                val userModule = elements.getTypeElement(userModuleFqcn)

                // Previous validation guarantees this annotation is present.
                val moduleAnnotation = userModule.getAnnotation(MODULE)!!
                // Dagger guarantees this property is present and is an array of types or errors.
                val includes = moduleAnnotation.getValue("includes", elements)!!
                    .cast<MirrorValue.Array>()
                    .filterIsInstance<MirrorValue.Type>()

                val generatedModuleName = userModule.toClassName().autoBinderModuleName()
                val referencesGeneratedModule = includes
                    .map { it.toTypeName() }
                    .any { it == generatedModuleName }
                if (!referencesGeneratedModule) {
                    error("@AutoBinderModule's @Module must include ${generatedModuleName.simpleName()}", userModule)
                }
            }
        }

        return false
    }

    private fun RoundEnvironment.findAutoBinderModuleElementsOrNull(): AutoBinderModuleElements? {
        val autoBinderModules = findElementsAnnotatedWith<AutoBinderModule>().castEach<TypeElement>()
        if (autoBinderModules.isEmpty()) {
            return null
        }
        if (autoBinderModules.size > 1) {
            autoBinderModules.forEach { error("Multiple @AutoBinderModule-annotated modules found.", it) }
            return null
        }

        val autoBinderModule = autoBinderModules.single()
        if (!autoBinderModule.hasAnnotation(MODULE)) {
            error("@AutoBinderModule must also be annotated as a Dagger @Module", autoBinderModule)
            return null
        }

        val factoryTypeElements = unprocessedBindNames.map(elements::getTypeElement)

        return AutoBinderModuleElements(autoBinderModule, factoryTypeElements)
    }

    private fun AutoBinderModuleElements.toAutoBinderModuleGenerator(
        roundEnvironment: RoundEnvironment
    ): AutoBinderModuleGenerator {
        val moduleName = moduleType.toClassName()
        val public = Modifier.PUBLIC in moduleType.modifiers
        return AutoBinderModuleGenerator(public, moduleName, roundEnvironment)
    }

    private fun writeAutoBinderModule(
        elements: AutoBinderModuleElements,
        moduleGenerator: AutoBinderModuleGenerator
    ) {
        val generatedTypeSpec = moduleGenerator.brewJava()
            .toBuilder()
            .addOriginatingElement(elements.moduleType)
            .applyEach(elements.bindElements) {
                addOriginatingElement(it)
            }
            .build()

        JavaFile.builder(moduleGenerator.generatedType.packageName(), generatedTypeSpec)
            .addFileComment("Generated by @AutoBinderModule. Do not modify!")
            .build()
            .writeTo(filer)
    }

    private fun error(message: String, element: Element? = null) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element)
    }

    private data class AutoBinderModuleElements(
        val moduleType: TypeElement,
        val bindElements: List<TypeElement>
    )
}