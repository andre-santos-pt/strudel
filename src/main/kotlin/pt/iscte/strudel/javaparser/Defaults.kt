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
    ForeignProcedure(null, "println", VOID, ANY) { m, args ->
        if (args.isEmpty())
            println()
        else
            println(args[0])
    },
    ForeignProcedure(null, "print", VOID, ANY) { m, args ->
        print(args[0])
    },
    ForeignProcedure("System.out", "println", VOID, ANY) { m, args ->
        if (args.isEmpty())
            println()
        else
            println(args[0])
    },
    ForeignProcedure("System.out", "print", VOID, ANY) { m, args ->
        if (args.isEmpty())
            println()
        else
            println(args[0])
    },
    ForeignProcedure(
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
        "concat",
        stringType,
        listOf(ANY, ANY)
    ) { m, args ->
        Value(stringType, args[0].value.toString() + args[1].value.toString())
    },
)