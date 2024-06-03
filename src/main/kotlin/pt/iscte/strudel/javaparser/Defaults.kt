package pt.iscte.strudel.javaparser

import pt.iscte.strudel.model.*
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.impl.ForeignProcedure
import pt.iscte.strudel.vm.impl.Value

internal val ObjectType = HostRecordType(Object::class.java.canonicalName)
internal val StringType = HostRecordType(String::class.java.canonicalName)

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

    Object::class.java.canonicalName to ObjectType.reference(),
    Object::class.java.canonicalName + "[]" to ObjectType.array().reference(),

    String::class.java.canonicalName to StringType.reference(),
    String::class.java.canonicalName + "[]" to StringType.array().reference()
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
        StringType,
        listOf(CHAR.array().reference())
    ) { m, args ->
            val chars = ((args[0].value as pt.iscte.strudel.vm.impl.Array).value as Array<IValue>).map { it.toChar() }.toCharArray()
            Value(StringType, String(chars))

    },
//    ForeignProcedure(
//        null,
//        null,
//        "concat",
//        stringType,
//        listOf(ANY, ANY)
//    ) { m, args ->
//        Value(stringType, args[0].value.toString() + args[1].value.toString())
//    },
//    ,
//    ForeignProcedure(
//        null,
//        null,
//        "length",
//        INT,
//        listOf(stringType)
//    ) { m, args ->
//        Value(INT, args[0].value.toString().length)
//    },
)