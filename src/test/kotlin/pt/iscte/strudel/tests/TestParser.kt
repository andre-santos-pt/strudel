package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.javaparser.Java2Strudel
import pt.iscte.strudel.model.cfg.createCFG
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertTrue

class TestParser {

    val src = """
        public class Main {
          public static int fact(int n) {
            int fact = 1;
          	for(int i=1; i<=n; i++){    
              fact=fact*i;    
          	}
            return fact;
          }
        }
    """

    @Test
    fun test() {
        val j2p = Java2Strudel()
        val m = j2p.load(src)
        println(m)

        m.blendBlockStatements()

        println(m)

        val vm: IVirtualMachine = IVirtualMachine.create()
        val cfg = m.procedures.first().createCFG()
        assertTrue(cfg.isValid(), System.lineSeparator() + cfg.nodes.joinToString(separator = System.lineSeparator()) { it.toString() })
        cfg.display()
        val r = vm.execute(m.procedures.first(), vm.getValue(5))
        println(r)
    }
}