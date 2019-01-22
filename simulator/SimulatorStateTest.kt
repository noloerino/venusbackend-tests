/* ktlint-disable package-name */
package venusbackend.simulator
/* ktlint-enable package-name */

import kotlin.test.Test
import kotlin.test.assertEquals

class SimulatorStateTest {
    @Test
    fun storeLoadRegister() {
        val state = SimulatorState32()
        state.setReg(1, 10)
        assertEquals(10, state.getReg(1))
        state.setReg(1, -10)
        assertEquals(-10, state.getReg(1))
    }

    @Test fun nowriteZeroRegister() {
        val state = SimulatorState32()
        state.setReg(0, 10)
        assertEquals(0, state.getReg(0))
    }
}
