/* ktlint-disable package-name */
package venusbackend.simulator
/* ktlint-enable package-name */

import kotlin.test.Test
import kotlin.test.assertEquals
import venusbackend.assembler.Assembler
import venus.vfs.VirtualFileSystem
import venusbackend.linker.Linker
import venusbackend.linker.ProgramAndLibraries

class UndoTest {
    @Test
    fun undoRegisterSet() {
        val (prog, _) = Assembler.assemble("""
        addi x8 x0 7
        addi x8 x0 9
        """)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.step()
        assertEquals(7, sim.getReg(8))
        sim.step()
        assertEquals(9, sim.getReg(8))
        sim.undo()
        assertEquals(7, sim.getReg(8))
        sim.step()
        assertEquals(9, sim.getReg(8))
    }

    @Test fun undoMemorySet() {
        val (prog, _) = Assembler.assemble("""
        addi x8 x0 100
        addi x9 x0 42
        sw x9 0(x8)
        """)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.step()
        sim.step()
        sim.step()
        assertEquals(42, sim.loadByte(100))
        sim.undo()
        assertEquals(0, sim.loadByte(100))
        sim.step()
        assertEquals(42, sim.loadByte(100))
    }

    @Test fun undoJump() {
        val (prog, _) = Assembler.assemble("""
        up: addi x8 x8 1
        j up
        """)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.step()
        assertEquals(1, sim.getReg(8))
        sim.step()
        assertEquals(1, sim.getReg(8))
        sim.undo()
        sim.undo()
        assertEquals(0, sim.getReg(8))
    }

    @Test fun undoMemoryLoad() {
        val (prog, _) = Assembler.assemble("""
        addi x8 x0 5
        sw x8 100(x0)
        lw x9 100(x0)
        """)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.step()
        assertEquals(5, sim.getReg(8))
        sim.step()
        sim.step()
        assertEquals(5, sim.getReg(9))
        sim.undo()
        assertEquals(0, sim.getReg(9))
    }
}
