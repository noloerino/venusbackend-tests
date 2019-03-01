/* ktlint-disable package-name */
package venusbackend.assembler
/* ktlint-enable package-name */

import kotlin.test.Test
import kotlin.test.assertTrue

class AssemblerErrorsTest {
    @Test
    fun noEmptyException() {
        val (_, errors) = Assembler.assemble("")
        assertTrue(errors.isEmpty())
    }

    @Test fun tooFewArguments() {
        val (_, errors) = Assembler.assemble("addi x1 x5")
        assertTrue(errors.isNotEmpty())
    }

    @Test fun immediateTooLarge() {
        val (_, errors) = Assembler.assemble("addi x1 x5 100000000")
        assertTrue(errors.isNotEmpty())
    }

    @Test fun immediateTooSmall() {
        val (_, errors) = Assembler.assemble("addi x1 x5 -100000000")
        assertTrue(errors.isNotEmpty())
    }

    @Test fun immediateNaN() {
        val (_, errors) = Assembler.assemble("addi x1 x5 foo")
        assertTrue(errors.isNotEmpty())
    }

    @Test fun registerNotARegister() {
        val (_, errors) = Assembler.assemble("addi blah x5 1")
        assertTrue(errors.isNotEmpty())
    }

    @Test fun badInstructionTest() {
        val (_, errors) = Assembler.assemble("nopi x0 x5 1")
        assertTrue(errors.isNotEmpty())
    }

    @Test fun registerIdTooBig() {
        val (_, errors) = Assembler.assemble("addi x32 x5 x1")
        assertTrue(errors.isNotEmpty())
    }

    @Test fun labelTwice() {
        val (_, errors) = Assembler.assemble("foo: nop\nfoo: nop")
        assertTrue(errors.isNotEmpty())
    }

    @Test fun numbericErrorLabels() {
        val (prog, errors) = Assembler.assemble("""
        1:  addi x1 x0 5
            j 2f
        """)
        assertTrue(errors.isNotEmpty())
    }

    @Test fun loadArgsAsRegError() {
        val (_, e, w) = Assembler.assemble("""lw s0 s0""")
        assertTrue(e.isEmpty())
        assertTrue(w.isNotEmpty())
    }

    @Test fun loadStoreArgsNotRegError() {
        val (_, e, w) = Assembler.assemble("""lw s0 hi""")
        assertTrue(e.isEmpty())
        assertTrue(w.isEmpty())
    }

//    @Test fun loadStoreMismatchedError() {
//        val (_, e, w) = Assembler.assemble("""lw s0 (4)x1""")
//        assertTrue(e.isNotEmpty())
//    }

//    @Test fun loadNoParenError() {
//        val (_, e, w) = Assembler.assemble("""lw s0 4 x1""")
//        assertTrue(e.isNotEmpty())
//    }

//    @Test fun StoreNoParenError() {
//        val (_, e, w) = Assembler.assemble("""sw s0 4 x1""")
//        assertTrue(e.isNotEmpty())
//    }

//    @Test fun loadMismatchParenError() {
//        val (_, e, w) = Assembler.assemble("""lw x0 (4)x1""")
//        assertTrue(e.isNotEmpty())
//    }

    @Test fun loadOnlyEndParenError() {
        val (_, e, w) = Assembler.assemble("""lw s0 4)x1""")
        assertTrue(e.isNotEmpty())
    }

    @Test fun loadFlippedParenError() {
        val (_, e, w) = Assembler.assemble("""lw s0 )4(x1""")
        assertTrue(e.isNotEmpty())
    }

    @Test fun ITypeWithParenError() {
        val (_, e, w) = Assembler.assemble("""addi x0 x0 (0)""")
        assertTrue(e.isNotEmpty())
    }

    @Test fun IDisplacedNotationError() {
        val (_, e, w) = Assembler.assemble("""addi x0 0(x0)""")
        assertTrue(e.isNotEmpty())
    }

    @Test fun jalrAsLoadStoreError() {
        val (_, e, w) = Assembler.assemble("""jalr x0 0(x0)""")
        assertTrue(e.isNotEmpty())
    }
}
