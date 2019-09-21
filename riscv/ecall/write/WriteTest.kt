/* ktlint-disable package-name */
package venusbackend.venusbackend.riscv.ecall.write
/* ktlint-enable package-name */

/* ktlint-disable no-wildcard-imports */
import venus.vfs.*
/* ktlint-enable no-wildcard-imports */
import venusbackend.assembler.Assembler
import venusbackend.linker.Linker
import venusbackend.linker.ProgramAndLibraries
import venusbackend.simulator.Simulator
import kotlin.test.Test
import kotlin.test.assertEquals

class WriteTest {
    @Test
    fun matrixwrite() {
        val (wrepo, wrepoe) = Assembler.assemble(write_repo, "write_repo.s")
        assertEquals(wrepoe.size, 0)
        val (wmatrix, wmatrixe) = Assembler.assemble(write_matrix, "write_matrix.s")
        assertEquals(wmatrixe.size, 0)
        val (ut, ute) = Assembler.assemble(utils, "utils.s")
        assertEquals(ute.size, 0)
        val PandL = ProgramAndLibraries(listOf(wrepo, wmatrix, ut), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.run()
        var f = sim.VFS.getObjectFromPath(output_file_name) ?: VFSDummy()
        assertEquals(f.type, VFSType.File)
        val bytes = (f as VFSFile).readText()
        if (bytes.length != expected_out.length) {
            assertEquals(expected_out, bytes)
        }
        val exp = expected_out
        var i = 0
        for (c in bytes) {
            val b = c.toShort().toByte()
            val e = expected_out[i].toShort().toByte()
            val truth = b == e
            if (!truth) {
                assertEquals(expected_out, bytes)
            }
            i++
        }
//        for (i in 0..bytes.length) {
//            val b = bytes[i].toShort().toByte()
//            val e = expected_out[i].toShort().toByte()
//            val truth = b == e
//            if (!truth) {
//                assertEquals(expected_out, bytes)
//            }
//        }
    }
}

val output_file_name = "final_output.bin"
val expected_out = "\u0003\u0000\u0000\u0000\u0003\u0000\u0000\u0000\u009a\u0099\u0099\u003f\u009a\u0099\u0019\u0040\u0033\u0033\u00b3\u0040\u00cd\u00cc\u00cc\u0040\u0033\u0033\u0013\u0040\u0000\u0000\u0090\u0040\u00cd\u00cc\u00cc\u0040\u0066\u0066\u00a6\u003f\u009a\u0099\u0059\u0040"
val write_repo = """.import write_matrix.s
.import utils.s

.data
output_path: .asciiz """" + output_file_name + """"
v0: .float 1.2 2.4 5.6 6.4 2.3 4.5 6.4 1.3 3.4

.text
main:
    la a0 output_path
    la a1 v0
    addi a2 x0 3
    addi a3 x0 3
    jal wm
    addi a0 x0 10
    ecall
"""
val write_matrix = """.globl wm

.data
error_string: .asciiz "Error occurred writing to or closing the file!"
.text
wm:
    addi sp sp -8
    mv t0 a0
    mv t1 a1
    mv t2 a2
    mv t3 a3
    addi a0 x0 13
    mv a1 t0
    addi a2 x0 1
    ecall
    mv t4 a0
    sw t2 0(sp)
    sw t3 4(sp)
    addi a0 x0 15
    mv a1 t4
    mv a2 sp
    addi a3 x0 8
    addi a4 x0 1
    ecall
    mul t5 t2 t3
    slli t5 t5 2
    addi a0 x0 15
    mv a1 t4
    mv a2 t1
    mv a3 t5
    addi a4 x0 1
    ecall
    bne a0 a3 eof_or_error
    addi a0 x0 16
    ecall
    bnez a0 eof_or_error
    jr ra
eof_or_error:
    addi a0 x0 4
    la a1 error_string
    ecall
"""
val utils = """.globl print_hex_array malloc

.data
newline: .asciiz "\n"

.text
malloc:
	# Call to sbrk
    mv a1 a0
    addi a0 x0 9
    ecall
    jr ra
print_hex_array:
	mv t0 a0
    mv t1 a1
    addi t2 x0 0
loop_start:
    beq t1 t2 loop_end
    slli t3 t2 2
    add t3 t0 t3
    addi a0 x0 34
    lw a1 0(t3) # load element from memory
    ecall
    addi a0 x0 4
    la a1 newline
    ecall
    addi t2 t2 1
    j loop_start
loop_end:
	jr ra
"""
