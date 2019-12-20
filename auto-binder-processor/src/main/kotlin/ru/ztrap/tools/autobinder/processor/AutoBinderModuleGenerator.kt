package ru.ztrap.tools.autobinder.processor

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import dagger.multibindings.ClassKey
import dagger.multibindings.IntKey
import dagger.multibindings.LongKey
import dagger.multibindings.StringKey
import ru.ztrap.tools.autobinder.core.AutoBindTo
import ru.ztrap.tools.autobinder.core.multibindings.AutoClassKey
import ru.ztrap.tools.autobinder.core.multibindings.AutoIntKey
import ru.ztrap.tools.autobinder.core.multibindings.AutoLongKey
import ru.ztrap.tools.autobinder.core.multibindings.AutoStringKey
import ru.ztrap.tools.autobinder.processor.internal.BINDS
import ru.ztrap.tools.autobinder.processor.internal.INTO_MAP
import ru.ztrap.tools.autobinder.processor.internal.INTO_SET
import ru.ztrap.tools.autobinder.processor.internal.MAP_KEY
import ru.ztrap.tools.autobinder.processor.internal.MODULE
import ru.ztrap.tools.autobinder.processor.internal.QUALIFIER
import ru.ztrap.tools.autobinder.processor.internal.applyEach
import ru.ztrap.tools.autobinder.processor.internal.castEach
import ru.ztrap.tools.autobinder.processor.internal.findElementsAnnotatedWith
import ru.ztrap.tools.autobinder.processor.internal.hasAnnotation
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
    roundEnvironment: RoundEnvironment,
    /** An optional `@Generated` annotation marker. */
    private val generatedAnnotation: AnnotationSpec? = null
) {
    private val bindElements = roundEnvironment.findAutoBinderElementsOrNull()

    val generatedType = moduleName.autoBinderModuleName()

    fun brewJava(): TypeSpec {
        return TypeSpec.classBuilder(generatedType)
            .apply { generatedAnnotation?.let { addAnnotation(it) } }
            .addAnnotation(MODULE)
            .addModifiers(ABSTRACT)
            .apply { if (public) addModifiers(PUBLIC) }
            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(PRIVATE)
                    .build()
            )
            .applyEach(bindElements) { addMethod(it.brewJava()) }
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
    val qualifier: AnnotationSpec?
    val mapKey: AnnotationSpec?
    val type: AutoBindTo.Type
    val translateQualifier: Boolean

    init {
        val annotation = annotatedType.getAnnotation(AutoBindTo::class.java)
        targetType = annotatedType.toClassName()
        bindType = getClassName { annotation.value.toClassName() }
        type = annotation.type
        qualifier = findAnnotatedAnnotation(annotatedType, QUALIFIER)
        mapKey = findAnnotatedAnnotation(annotatedType, MAP_KEY)
        translateQualifier = annotation.translateQualifier
    }

    private val bindMethodName: String
        get() = "bind${targetType.joinedSimpleNames}To${bindType.joinedSimpleNames}"

    private fun findAnnotatedAnnotation(element: TypeElement, annotation: ClassName): AnnotationSpec? {
        return element.annotationMirrors
            .firstOrNull { it.annotationType.asElement().hasAnnotation(annotation) }
            ?.let(AnnotationSpec::get)
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

    private fun actualKeyType(typeName: TypeName): TypeName = when (typeName.toString()) {
        AutoClassKey::class.java.canonicalName -> ClassKey::class.toClassName()
        AutoStringKey::class.java.canonicalName -> StringKey::class.toClassName()
        AutoIntKey::class.java.canonicalName -> IntKey::class.toClassName()
        AutoLongKey::class.java.canonicalName -> LongKey::class.toClassName()
        else -> typeName
    }

    fun brewJava(): MethodSpec {
        return MethodSpec.methodBuilder(bindMethodName)
            .addAnnotation(BINDS)
            .addModifiers(ABSTRACT)
            .returns(bindType)
            .addParameter(targetType, "binding")
            .apply {
                if (translateQualifier) {
                    qualifier?.let { addAnnotation(it) }
                }

                when (type) {
                    AutoBindTo.Type.INTO_SET -> addAnnotation(INTO_SET)
                    AutoBindTo.Type.INTO_MAP -> addAnnotation(INTO_MAP)
                    AutoBindTo.Type.DEFAULT -> Unit // add nothing
                }

                mapKey?.let {
                    var annotation = it
                    val actualType = actualKeyType(it.type)
                    if (actualType != it.type) {
                        annotation = AnnotationSpec.builder(actualType.toClassName())
                            .apply {
                                it.members.forEach { (key, listOfCodeBlocks) ->
                                    listOfCodeBlocks.forEach { codeBlock ->
                                        addMember(key, codeBlock)
                                    }
                                }
                            }
                            .build()
                    }
                    addAnnotation(annotation)
                }
            }
            .build()
    }
}

fun ClassName.autoBinderModuleName(): ClassName {
    return ClassName.get(packageName(), simpleNames().joinToString("_", prefix = "AutoBinder_"))
}
