package pt.iscte.strudel.examples

import pt.iscte.strudel.javaparser.Java2Strudel

fun main() {
    val javaCode = """
        class ArraySearch {
            static boolean linearSearch(int[] a, int e) {
                for(int i = 0; i < a.length; i++)
                    if(a[i] == e)
                        return true;
                return false;
            }
            static boolean binarySearch(int[] a, int e) {
                int l = 0;
                int r = a.length - 1;
                while (l <= r) {
                    int m = l + ((r - l) / 2);
                    if (a[m] == e) return true;
                    if (a[m] < e) l = m + 1;
                    else r = m - 1;
                }
                return false;
            }
        }
    """.trimIndent()
    val module = Java2Strudel().load(javaCode)
    println(module)
    val binarySearch = module.getProcedure { it.id == "binarySearch" }
    println(binarySearch)
}