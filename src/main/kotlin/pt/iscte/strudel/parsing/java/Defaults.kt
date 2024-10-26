package pt.iscte.strudel.parsing.java

import pt.iscte.strudel.model.*
import pt.iscte.strudel.parsing.java.extensions.getString
import pt.iscte.strudel.vm.*
import pt.iscte.strudel.vm.impl.ForeignProcedure
import pt.iscte.strudel.vm.impl.Reference
import pt.iscte.strudel.vm.impl.Value

internal val ObjectType = HostRecordType(Object::class.java.canonicalName)
internal val StringType = HostRecordType(String::class.java.canonicalName)

internal val defaultTypes = mapOf(
    "void" to VOID,

    "int" to INT,
    "double" to DOUBLE,
    "char" to CHAR,
    "boolean" to BOOLEAN,

    "long" to INT,
    "float" to DOUBLE,

    "int[]" to INT.array().reference(),
    "double[]" to DOUBLE.array().reference(),
    "char[]" to CHAR.array().reference(),
    "boolean[]" to BOOLEAN.array().reference(),

    "int[][]" to INT.array().reference().array().reference(),
    "double[][]" to DOUBLE.array().reference().array().reference(),
    "char[][]" to CHAR.array().reference().array().reference(),
    "boolean[][]" to BOOLEAN.array().reference().array().reference(),

    Object::class.java.canonicalName to ObjectType.reference(),
    Object::class.java.canonicalName + "[]" to ObjectType.reference().array().reference(),

    String::class.java.canonicalName to StringType.reference(),
    String::class.java.canonicalName + "[]" to StringType.reference().array().reference()
)

internal const val NEW_STRING = "\$newString"

internal val newString = ForeignProcedure(
    null,
    null,
    NEW_STRING,
    StringType.reference(),
    listOf(CHAR.array().reference())
) { _, args ->
    val chars =
        ((args[0].value as pt.iscte.strudel.vm.impl.Array).value as Array<IValue>).map { it.toChar() }
            .toCharArray()
    Reference(Value(StringType, String(chars)))
}

internal val defaultForeignProcedures = listOf(
    ForeignProcedure(null, null, "println", VOID, ANY) { m, args ->
        val text = if (args.isEmpty()) System.lineSeparator()
        else args[0].toString() + System.lineSeparator()
        m.systemOutput(text)
    },
    ForeignProcedure(
        null,
        null,
        "print",
        VOID,
        ANY
    ) { m, args -> m.systemOutput(args[0].toString()) },
    ForeignProcedure(null, "System.out", "println", VOID, ANY) { m, args ->
        val text = if (args.isEmpty()) System.lineSeparator()
        else args[0].toString() + System.lineSeparator()
        m.systemOutput(text)
    },
    ForeignProcedure(
        null,
        "System.out",
        "print",
        VOID,
        ANY
    ) { m, args -> m.systemOutput(args[0].toString()) },
    newString
)

fun IVirtualMachine.allocateString(content: String): IReference<IRecord> {
    val ref = execute(
        newString,
        allocateArrayOf(CHAR, *content.toCharArray().toTypedArray())
    )
    ref as IReference<IRecord>
    listeners.forEach { it.recordAllocated(ref) }
    return ref
}

fun IVirtualMachine.allocateStringArray(vararg list: String): IReference<IArray> {
    val ref = allocateArray(StringType.reference(), list.size)
    list.forEachIndexed { i, s ->
        ref.target.setElement(i, getString(s))
    }
    listeners.forEach { it.arrayAllocated(ref) }
    return ref
}