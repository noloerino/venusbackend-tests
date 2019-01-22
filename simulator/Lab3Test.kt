/* ktlint-disable package-name */
package venusbackend.simulator
/* ktlint-enable package-name */

import kotlin.test.Test
import kotlin.test.assertEquals
import venusbackend.assembler.Assembler
import venus.vfs.VirtualFileSystem
import venusbackend.linker.Linker
import venusbackend.linker.ProgramAndLibraries

class Lab3Test {
    @Test
    fun Ex1() {
        val (prog, _) = Assembler.assemble(
"""
        .data
        .word 2, 4, 6, 8
n:      .word 9


        .text
main: 		add     t0, x0, x0
		addi    t1, x0, 1
		la      t3, n
		lw      t3, 0(t3)
fib: 		beq     t3, x0, finish
		add     t2, t1, t0
		mv      t0, t1
		mv      t1, t2
		addi    t3, t3, -1
		jal     x0, fib
finish: addi    a0, x0, 1
		addi    a1, t0, 0
		# ecall
		addi    a0, x0, 10
		ecall
"""
        )
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.run()
        assertEquals(34, sim.getReg(11))
    }

    @Test
    fun Ex1_64() {
        val (prog, _) = Assembler.assemble(
"""
        .data
        .word 2, 4, 6, 8
n:      .word 9


        .text
main: 		add     t0, x0, x0
		addi    t1, x0, 1
		la      t3, n
		lw      t3, 0(t3)
fib: 		beq     t3, x0, finish
		add     t2, t1, t0
		mv      t0, t1
		mv      t1, t2
		addi    t3, t3, -1
		jal     x0, fib
finish: addi    a0, x0, 1
		addi    a1, t0, 0
		# ecall
		addi    a0, x0, 10
		ecall
"""
        )
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked, state = SimulatorState64())
        sim.run()
        assertEquals(34L, sim.getReg(11).toLong())
    }
}
