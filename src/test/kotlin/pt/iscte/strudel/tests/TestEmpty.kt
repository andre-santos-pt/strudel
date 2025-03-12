package pt.iscte.strudel.tests

import org.junit.jupiter.api.Test
import pt.iscte.strudel.model.VOID
import pt.iscte.strudel.model.dsl.Procedure

class TestEmpty : BaseTest({
    Procedure(VOID, "empty") {

    }
}){

    @Test
    fun execute() {
        vm.execute(procedure)
    }

}