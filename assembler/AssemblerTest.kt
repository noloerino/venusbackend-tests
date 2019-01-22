/* ktlint-disable package-name */
package venusbackend.assembler
/* ktlint-enable package-name */

import kotlin.test.Test
import venus.vfs.VirtualFileSystem
import kotlin.test.assertEquals
import venusbackend.linker.Linker
import venusbackend.linker.ProgramAndLibraries
import venusbackend.simulator.Simulator
import venusbackend.simulator.SimulatorState64

class AssemblerTest {
    @Test
    fun assembleLexerTest() {
        val (prog, _) = Assembler.assemble("""
        addi x1 x0 5
        addi x2 x1 5
        add x3 x1 x2
        andi x3 x3 8
        """)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.run()
        assertEquals(8, sim.getReg(3))
    }

    @Test
    fun assembleLexer64Test() {
        val (prog, _) = Assembler.assemble("""
        addi x1 x0 5
        addi x2 x1 5
        add x3 x1 x2
        andi x3 x3 8
        """)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked, state = SimulatorState64())
        sim.run()
        assertEquals(8L, sim.getReg(3).toLong())
    }

    @Test fun storeLoadTest() {
        val (prog, _) = Assembler.assemble("""
        addi x1 x0 100
        sw x1 60(x0)
        lw x2 -40(x1)
        """)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.step()
        assertEquals(100, sim.getReg(1))
        sim.step()
        assertEquals(100, sim.getReg(1))
        assertEquals(100, sim.loadWord(60))
        sim.step()
        assertEquals(100, sim.getReg(2))
    }

    @Test fun storeLoad64Test() {
        val (prog, _) = Assembler.assemble("""
        addi x1 x0 100
        sw x1 60(x0)
        lw x2 -40(x1)
        """)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked, state = SimulatorState64())
        sim.step()
        assertEquals(100L, sim.getReg(1).toLong())
        sim.step()
        assertEquals(100L, sim.getReg(1).toLong())
        assertEquals(100, sim.loadWord(60))
        sim.step()
        assertEquals(100L, sim.getReg(2).toLong())
    }

    @Test fun branchTest() {
        val (prog, _) = Assembler.assemble("""
        add x8 x8 x9
        addi x7 x0 5
        start: add x8 x8 x9
        addi x9 x9 1
        bne x9 x6 start
        """)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        for (i in 1..17) sim.step()
        assertEquals(10, sim.getReg(8))
    }

    @Test fun branch64Test() {
        val (prog, _) = Assembler.assemble("""
        add x8 x8 x9
        addi x7 x0 5
        start: add x8 x8 x9
        addi x9 x9 1
        bne x9 x6 start
        """)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked, state = SimulatorState64())
        for (i in 1..17) sim.step()
        assertEquals(10L, sim.getReg(8).toLong())
    }

    @Test fun otherImmediateTest() {
        val (prog, _) = Assembler.assemble("""
        addi x8 x8 0xf7
        addi x9 x9 0b10001
        """)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.step()
        assertEquals(0xf7, sim.getReg(8))
        sim.step()
        assertEquals(0b10001, sim.getReg(9))
    }

    @Test fun otherImmediate64Test() {
        val (prog, _) = Assembler.assemble("""
        addi x8 x8 0xf7
        addi x9 x9 0b10001
        """)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked, state = SimulatorState64())
        sim.step()
        assertEquals(0xf7L, sim.getReg(8).toLong())
        sim.step()
        assertEquals(0b10001L, sim.getReg(9).toLong())
    }

    @Test fun alignTest() {
        val (prog, _) = Assembler.assemble("""
        .data
        .align 3
        one: # 8-byte aligned
        .byte 1
        .align 3
        two: # 8-byte aligned
        .byte 2
        .align 2
        three: # 4-byte aligned
        .byte 3
        .text
        la a1, one
        la a2, two
        la a3, three
        sub x5, a2, a1  # Should be 8
        sub x6, a3, a2  # Should be 4
        """)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.run()
        assertEquals(8, sim.getReg(5))
        assertEquals(4, sim.getReg(6))
    }

    @Test fun numbericLabels() {
        val (prog, _) = Assembler.assemble("""
        1:  addi x1 x0 5
            addi x3 x0 2
            addi x2 x0 0
            j 2f
            addi x1 x1 5
        2:  addi x1 x1 5
            addi x2 x2 1
            beq x2 x3 3f
            j 2b
        3:  addi x3 x3 1
        3:  nop
        2:  nop
        1:  nop
        """)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.run()
        assertEquals(15, sim.getReg(1))
        assertEquals(2, sim.getReg(2))
        assertEquals(3, sim.getReg(3))
    }
}
