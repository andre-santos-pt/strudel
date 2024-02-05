package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite
import pt.iscte.strudel.tests.javaparser.*
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

@Suite
@SelectClasses(
    TestEmpty::class,

    // ifs
    TestAbsInt::class,
    TestAbsDouble::class,
    TestIsEven::class,

    // unary operators
    TestNot::class,
    TestRound::class,
    TestUnaryPlus::class,

    // recursion
    TestFactorial::class,

    // array functions
    TestSumDoubleArray::class,
    TestMaxArray::class,
    TestNaturals::class,
    TestArrayExists::class,
    TestCharArrayCount::class,
    TestSumPositivesArrayContinue::class,
    TestBinarySearch::class,

    // array procedures
    TestSwap::class,
    TestReplaceFirstReturn::class,
    TestReplaceFirstBreak::class,
    TestInvert::class,
    //	TestSelectionSort.class,

    // array errors
    TestArrayIndexOutOfBounds::class,
    TestDivByZeroInt::class,
    TestDivByZeroIntMod::class,
    TestDivByZeroDouble::class,

    TestLoopIterationMax::class,

    // matrix functions
    TestMatrixSum::class,
    TestMatrixIdentity::class,
    //	TestMatrixTranspose.class,
    // multiplication

    // matrix procedures
    TestMatrixScalar::class,
    // swap lines

    //	TestIsSame.class

    TestArrayList::class,
    TestLinkedList::class,

    TestAvgInvoke::class,
   // TestUnboundType::class,
    TestBuiltinRound::class,
    TestJDKLinkedList::class,
    TestBuiltinString::class,

    TestConditional::class,
    TestConditionalAnd::class,
    TestConditionalOr::class,

    //--- direct source
    TestSortingAlgorithms::class,

    TestThrow::class,

    TestSystemOut::class,
    TestDynamicDispatch::class,

    TestFieldInitializers::class,
    TestDependentFieldDeclarations::class,

    TestAutomaticForeignProcedure::class,

    TestConditionalJava::class,
    TestLogicalOperatorsJava::class,


    TestMultipleFieldDeclarations::class,

    TestMultipleVariableDeclarations::class,

    TestSameIdentifierVariables::class,

    TestComparable::class,
    TestComparator::class,
    TestOwnComparable::class,
    TestComparablePolymorphicParameter::class,

    /* All of these were missing: check main() function below

    TestJavaParser::class,
    TestChars::class,
    TestMatrix3d::class,
    TestNaturalsTemp::class,
    TestParser::class,
    TestsPaper::class,
    TestStackRecord::class,
    TestUnboundType::class,
    TestDocumentation::class,
    TestGCD::class,
    TestHeavyProcessing::class,
    TestMatrixIndexOutOfBounds::class,
    TestUsedMemory::class
     */

)
class AllTests

fun main() {
    /**
     * Prints a list of all the valid test classes.
     *
     * onlyMissing = true: Prints only a list of valid test classes not included in @SelectClasses above.
     * onlyMissing = false: Prints a list of all valid test classes.
     */
    val onlyMissing = true

    val suite: List<KClass<*>> = AllTests::class.findAnnotation<SelectClasses>()!!.value.toList()

    val tests = File("src/test/kotlin")
    val files: List<File> = tests.walk().filter { it.isFile && it.extension == "kt" }.toList()

    val classes: MutableList<KClass<*>> = mutableListOf()
    files.forEach { file ->
        val name = file.relativeTo(tests).path.replace(File.separatorChar, '.').removeSuffix(".kt")
        runCatching { classes.add(Class.forName(name).kotlin) }
    }

    val matches = classes.filter { cls ->
        (!onlyMissing || cls !in suite) && cls.members.any { it.hasAnnotation<Test>() }
    }

    if (onlyMissing)
        println("AllTests is missing the following classes:\n")
    else
        println("AllTests can be defined for the following classes:\n")

    println(matches.joinToString(",\n") { "${it.simpleName}::class" })
}