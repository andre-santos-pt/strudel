# Strudel: A Library for Modeling and Simulation of Structured Programming

## What is Strudel?
**Strudel** is a software language in the form of a programming
library that comprises classes that model structured
programming, where their instances describe models of programs.
In addition to performing static analysis, Strudel provides
a virtual machine capable of interpreting those models,
through a step-by-step simulation of a call stack-based execution,
where one may programmatically observe every
aspect of execution in detail, such as errors, tracking variables,
loop iterations, call stack, or memory allocation.

<br>

## How does it work?
At the heart of Strudel there is a meta-model addressing concepts
for describing constructs of structured programming.
These constructs are in essence those of a While language
(also often referred to as IMP language), with additional
elements defining and calling procedures. These models are
the shared abstraction for defining, analyzing, and executing
programs.

These abstractions are encoded
as a programming library (Strudel) implemented in
Kotlin, suitable to be used with JVM languages. By instantiating
the classes modeling these abstractions, one defines a
set of procedures. These can be executed, simulating the computational
process based on a call stack and heap memory.
This feature is embodied in the library as a Virtual Machine
(VM) that can be used programmatically.

<br>

## Examples
The following are simple examples that showcase Strudel's basic functionalities. For a more complete set of examples,
check out [the examples folder](src/main/kotlin/pt/iscte/strudel/examples).

### Using Strudel's DSL to instantiate procedure models
Strudel includes an internal DSL (domain-specific language) which makes
use of Kotlin extension functions that apply lambdas with
receivers. This feature enables the representation of the nested
structures in a way that resembles the usual programming
language syntax. This feature also makes use of operator overloading
for arithmetic operations and infix syntax for relational
and logical operators.

The following code utilizes Strudel's DSL to instantiate the model for a function that performs binary search on an array of 
integers.
```kotlin
// Instantiate a procedure that returns a boolean value
val binarySearch = Procedure(BOOLEAN) {
    val a = Param(array(INT)) // Function parameter of type int[]
    val e = Param(INT) // Function parameter of type int
    
    val l = Var(INT, 0) // int l = 0
    val r = Var(INT, Length(a) - 1) // int r = a.length - 1
    While(l smallerEq r) {
        val m = Var(INT, l + (r - l) / 2) // m = l + (r - l) / 2
        If(a[m] equal e) {
            Return(True)
        }
        If(a[m] smaller e) {
            Assign(l, m + 1) // l = m + 1
        }.Else {
            Assign(r, m - 1) // r = m - 1
        }
    }
    Return(False)
    
}
```
The following code is a direct translation of the previous model to typical Java syntax.
```java
public class Test {
    public static boolean binarySearch(int[] a, int e) {
        int l = 0;
        int r = a.length - 1;
        while (l <= r) {
            int m = l + (r - l) / 2;
            if (a[m] == e) {
                return true;
            }
            if (a[m] < e) {
                l = m + 1;
            } else {
                r = m - 1;
            }
        }
        return false;
    }
}
```

### Loading and executing Java code through Strudel
You can start by loading a Java source code file to Strudel to obtain a **module**, from which you can get the 
**procedure** you want to execute:
```kotlin
val f = File("BinarySearch.java")
val module = Java2Strudel().load(f)
val search = module.getProcedure("binarySearch")
```
You can then create a Strudel virtual machine and use it to run the procedure with the arguments you wish to pass
to your function:

```kotlin
val vm: IVirtualMachine = IVirtualMachine.create()

val a: IReference<IArray> = vm.allocateArrayOf(INT, 1, 3, 5, 7, 11, 13, 17, 23, 27)
val e: IValue = vm.getValue(23)

val result: IValue? = vm.execute(search, a, e) // true
```
