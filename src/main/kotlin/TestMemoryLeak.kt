import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

import pt.iscte.strudel.parsing.java.Java2Strudel

@OptIn(ExperimentalTime::class)
fun main() {
    val src = """
         class Test {
            int[] naturals(int n) {
                int[] nats = new int[n];
                int i = 0;
                while(i < n) {
                    nats[i] = i + 1;
                    i = i + 1;
                }        
                return nats;
            } 
         }
    """.trimIndent()
    val time = measureTime {
        repeat(1E6.toInt()) {
            val ct = measureTime {
                Java2Strudel(checkJavaCompilation = true, bindSource = true, bindJavaParser = true).load(src)
            }
            print("\r${it + 1} / 1000000")
        }
    }
    println("Done! Took $time.")
}