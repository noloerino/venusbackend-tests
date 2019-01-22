/* ktlint-disable package-name */
package venusbackend.assembler
/* ktlint-enable package-name */

import kotlin.test.Test
import venus.vfs.VirtualFileSystem
import kotlin.test.assertTrue
import venusbackend.simulator.Simulator
import venusbackend.linker.Linker
import venusbackend.linker.ProgramAndLibraries

class AssemblerSpeedTest {
    @Test
    fun nopRepeat() {
        val (prog, _) = Assembler.assemble("nop\n".repeat(1000))
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.run()
        assertTrue(true)
    }
}
