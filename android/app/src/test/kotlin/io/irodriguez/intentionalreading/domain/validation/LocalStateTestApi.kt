package io.irodriguez.intentionalreading.domain.validation

import java.io.File
import java.lang.reflect.InvocationTargetException

internal object LocalStateTestApi {
    private const val VALIDATOR_CLASS =
        "io.irodriguez.intentionalreading.domain.validation.LocalStateValidator"
    private const val STORE_CLASS =
        "io.irodriguez.intentionalreading.data.local.state.LocalStateStore"
    private const val FILE_CLASS =
        "io.irodriguez.intentionalreading.data.local.state.LocalStateFile"

    fun validate(bytes: ByteArray): Any = call(newInstance(VALIDATOR_CLASS), "validate", bytes)

    fun store(directory: File): Any = newInstance(STORE_CLASS, directory)

    fun storeThatFailsBeforeRename(directory: File): Any {
        val fileClass = productionClass(FILE_CLASS)
        val callback: () -> Unit = { error("injected failure before rename") }
        val file = construct(fileClass, directory, callback)
        return construct(productionClass(STORE_CLASS), file)
    }

    fun call(instance: Any, methodName: String, vararg arguments: Any): Any {
        val method = instance.javaClass.methods.singleOrNull {
            it.name == methodName && it.parameterCount == arguments.size
        } ?: instance.javaClass.declaredMethods.single {
            it.name.substringBefore('-') == methodName && it.parameterCount == arguments.size
        }
        method.isAccessible = true
        return try {
            method.invoke(instance, *arguments)
                ?: error("$methodName unexpectedly returned null")
        } catch (failure: InvocationTargetException) {
            throw failure.targetException
        }
    }

    fun property(instance: Any, propertyName: String): Any {
        val getterName = "get" + propertyName.replaceFirstChar(Char::uppercaseChar)
        return call(instance, getterName)
    }

    fun successState(result: Any): Any {
        check(result.javaClass.simpleName == "Success") {
            "Expected LocalStateResult.Success but was ${result.javaClass.name}"
        }
        return property(result, "state")
    }

    fun failureCode(result: Any): String {
        check(result.javaClass.simpleName == "Failure") {
            "Expected LocalStateResult.Failure but was ${result.javaClass.name}"
        }
        return property(result, "code").toString()
    }

    fun source(result: Any): String = property(result, "source").toString()

    private fun newInstance(className: String, vararg arguments: Any): Any =
        construct(productionClass(className), *arguments)

    private fun construct(type: Class<*>, vararg arguments: Any): Any {
        val constructor = type.declaredConstructors.singleOrNull { candidate ->
            candidate.parameterCount == arguments.size && candidate.parameterTypes.zip(arguments).all {
                (parameter, argument) -> parameter.isAssignableFrom(argument.javaClass)
            }
        } ?: error("No ${type.name} constructor accepts ${arguments.map { it.javaClass.name }}")
        constructor.isAccessible = true
        return try {
            constructor.newInstance(*arguments)
        } catch (failure: InvocationTargetException) {
            throw failure.targetException
        }
    }

    private fun productionClass(name: String): Class<*> = try {
        Class.forName(name)
    } catch (failure: ClassNotFoundException) {
        throw IllegalStateException("Missing Slice 1 production behavior: $name", failure)
    }
}
