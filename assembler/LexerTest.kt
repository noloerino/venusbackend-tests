/* ktlint-disable package-name */
package venusbackend.assembler
/* ktlint-enable package-name */

import venusbackend.assertArrayEquals
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class LexerTest {
    @Test
    fun basicLexing() {
        val line = "add x0 x0 x0"
        val (labels, args) = Lexer.lexLine(line)
        assertTrue(labels.isEmpty(), "Array is not empty!")
        assertArrayEquals(listOf("add", "x0", "x0", "x0"), args)
    }

    @Test fun lexLabel() {
        val line = "start: add x2 x2 x3"
        val (labels, args) = Lexer.lexLine(line)
        assertArrayEquals(listOf("start"), labels)
        assertArrayEquals(listOf("add", "x2", "x2", "x3"), args)
    }

    @Test fun lexComment() {
        val line = "add x0 x0 x0 # hi: x0"
        val (labels, args) = Lexer.lexLine(line)
        assertTrue(labels.isEmpty(), "Array is not empty!")
        assertArrayEquals(listOf("add", "x0", "x0", "x0"), args)
    }

    @Test fun lexComma() {
        val line = "add x0, x1, x2"
        val (labels, args) = Lexer.lexLine(line)
        assertTrue(labels.isEmpty(), "Array is not empty!")
        assertArrayEquals(listOf("add", "x0", "x1", "x2"), args)
    }

    @Test fun lexLabelSpace() {
        val line = "  \t  start: add x0, x1, x2"
        val (labels, args) = Lexer.lexLine(line)
        assertArrayEquals(listOf("start"), labels)
        assertArrayEquals(listOf("add", "x0", "x1", "x2"), args)
    }

    @Test fun lexBaseDisplacement() {
        val line = "sw x1 0(x2)"
        val (labels, args) = Lexer.lexLine(line)
        assertTrue(labels.isEmpty(), "Array is not empty!")
        assertArrayEquals(listOf("sw", "x1", "0", "x2"), args)
    }

    @Test fun lexNothing() {
        val line = ""
        val (label, args) = Lexer.lexLine(line)
        assertTrue(label.isEmpty(), "Array is not empty!")
        assertTrue(args.isEmpty(), "Array is not empty!")
    }

    @Test fun lexAsciizBadStrings() {
        val (_, errors1) = Assembler.assemble("""
        .data
        .asciiz   unquoted
        .text
        nop
        """)
        assertTrue(errors1.isNotEmpty())

        val (_, errors2) = Assembler.assemble("""
        .data
        .asciiz   "no end quote
        .text
        nop
        """)
        assertTrue(errors2.isNotEmpty())

        val (_, errors3) = Assembler.assemble("""
        .data
        .asciiz   "good" junk
        .text
        nop
        """)
        assertTrue(errors3.isNotEmpty())
    }

    @Test fun lexMultipleLabels() {
        val line = "hello: world: label: me:"
        val (labels, args) = Lexer.lexLine(line)
        assertArrayEquals(listOf("hello", "world", "label", "me"), labels)
        assertTrue(args.isEmpty(), "Array is not empty!")
    }

    @Test fun lexMultipleLabelsAndInstruction() {
        val line = "hello: world: label: me: lui 2"
        val (labels, args) = Lexer.lexLine(line)
        assertArrayEquals(listOf("hello", "world", "label", "me"), labels)
        assertArrayEquals(listOf("lui", "2"), args)
    }

    @Test fun lexDelimiterInChar() {
        val line = "addi x0 x0 ':'"
        val (labels, args) = Lexer.lexLine(line)
        assertTrue(labels.isEmpty(), "Array is not empty!")
        assertArrayEquals(listOf("addi", "x0", "x0", "':'"), args)
    }

    @Test fun lexColonInComment() {
        val line = "#hello:"
        val (labels, args) = Lexer.lexLine(line)
        assertTrue(labels.isEmpty(), "Array is not empty!")
        assertTrue(args.isEmpty(), "Array is not empty!")
    }

    @Test fun lexColonInAsciiz() {
        val line = """.asciiz "hi:"""
        val (labels, _) = Lexer.lexLine(line)
        assertTrue(labels.isEmpty(), "Array is not empty!")
    }

    @Test fun lexLabelAndComment() {
        val line = "hello: # hi!"
        val (labels, _) = Lexer.lexLine(line)
        assertArrayEquals(listOf("hello"), labels)
    }

    @Test fun labelInInstruction() {
        val line = "add label: x0 x0"
        try {
            Lexer.lexLine(line)
            fail("did not error on label in the middle of an instruction")
        } catch (e: AssemblerError) {
            assertTrue(true)
        }
    }
}
