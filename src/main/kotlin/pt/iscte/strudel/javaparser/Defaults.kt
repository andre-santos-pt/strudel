package pt.iscte.strudel.javaparser

import pt.iscte.strudel.model.*
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.impl.ForeignProcedure
import pt.iscte.strudel.vm.impl.Value

internal val objectType = HostRecordType(Object::class.java.name)
internal val stringType = HostRecordType(String::class.java.name)

internal val defaultTypes = mapOf(
    "void" to VOID,

    //"Object" to ANY,

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

    Object::class.java.simpleName to objectType.reference(),
    Object::class.java.simpleName + "[]" to objectType.array().reference(),

    String::class.java.simpleName to stringType.reference(),
    String::class.java.simpleName + "[]" to stringType.array().reference()
)

internal const val NEW_STRING = "\$newString"

internal val defaultForeignProcedures = listOf(
    ForeignProcedure(null, null, "println", VOID, ANY) { m, args ->
        val text = if(args.isEmpty()) System.lineSeparator()
        else args[0].toString() + System.lineSeparator()
        m.systemOutput(text)
    },
    ForeignProcedure(null,null, "print", VOID, ANY) { m, args ->
        m.systemOutput(args[0].toString())
    },
    ForeignProcedure(null,"System.out", "println", VOID, ANY) { m, args ->
        val text = if(args.isEmpty()) System.lineSeparator()
        else args[0].toString() + System.lineSeparator()
        m.systemOutput(text)
    },
    ForeignProcedure(null,"System.out", "print", VOID, ANY) { m, args ->
        m.systemOutput(args[0].toString())
    },
    ForeignProcedure(
        null,
        null,
        NEW_STRING,
        stringType,
        listOf(CHAR.array().reference())
    ) { m, args ->
            val chars = ((args[0].value as pt.iscte.strudel.vm.impl.Array).value as Array<IValue>).map { it.toChar() }.toCharArray()
            Value(stringType, String(chars))

    },
    ForeignProcedure(
        null,
        null,
        "concat",
        stringType,
        listOf(ANY, ANY)
    ) { m, args ->
        Value(stringType, args[0].value.toString() + args[1].value.toString())
    },
    ForeignProcedure(
        null,
        null,
        "length",
        INT,
        listOf(stringType)
    ) { m, args ->
        Value(INT, args[0].value.toString().length)
    },
)