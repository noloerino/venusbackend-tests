/* ktlint-disable package-name */
package venusbackend.simulator
/* ktlint-enable package-name */

import venus.Renderer
import venusbackend.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MemoryTest {
    private fun assertHexEquals(expected: Number, actual: Number) {
        assertEquals(expected, actual, "expected ${Renderer.toHex(expected)}, got ${Renderer.toHex(actual)}")
    }

    private fun storeAndCheckHalfWord(memory: Memory, addr: ByteAddress, value: Int) {
        memory.storeHalfWord(addr, value)
        assertHexEquals(value and 0xFFFF, memory.loadHalfWord(addr))
        assertHexEquals(value and 0xFF, memory.loadByte(addr))
        assertHexEquals((value shr 8) and 0xFF, memory.loadByte(addr + 1))
    }

    private fun storeAndCheckWord(memory: Memory, addr: ByteAddress, value: Int) {
        memory.storeWord(addr, value)
        assertHexEquals(value, memory.loadWord(addr))
        assertHexEquals(value and 0xFFFF, memory.loadHalfWord(addr))
        assertHexEquals((value shr 16) and 0xFFFF, memory.loadHalfWord(addr + 2))
        for (i in 0..3) {
            assertHexEquals((value shr (8 * i)) and 0xFF, memory.loadByte(addr + i))
        }
    }

    private fun storeAndCheckLong(memory: Memory, addr: ByteAddress, value: Long) {
        memory.storeLong(addr, value)
        assertHexEquals(value, memory.loadLong(addr))
        assertHexEquals((value and 0xFFFFFFFF).toInt(), memory.loadWord(addr))
        assertHexEquals(((value shr 32) and 0xFFFFFFFF).toInt(), memory.loadWord(addr + 4))
        for (i in 0..3) {
            assertHexEquals((value shr (16 * i)).toInt() and 0xFFFF, memory.loadHalfWord(addr + 2 * i))
        }
        for (i in 0..7) {
            assertHexEquals((value shr (8 * i)).toInt() and 0xFF, memory.loadByte(addr + i))
        }
    }

    @Test fun byteAddrWordOffsetMaskTest() {
        var addr: ByteAddress = 0xfff_fff00
        // hex literals starting with 0xff overflow positive signed int
        assertHexEquals(0x0000_00ff, addr.wordOffsetMask())
        addr += 1
        assertHexEquals(0x0000_ff00, addr.wordOffsetMask())
        addr += 1
        assertHexEquals(0x00ff_0000, addr.wordOffsetMask())
        addr += 1
        assertHexEquals(0xff00_0000.toInt(), addr.wordOffsetMask())
    }

    @Test fun byteStoreLoadTest() {
        val memory = Memory()
        memory.storeByte(100, 42)
        assertHexEquals(42, memory.loadByte(100))
    }

    @Test fun halfwordStoreLoadTest() {
        val memory = Memory()
        memory.storeHalfWord(100, 0xdead)
        storeAndCheckHalfWord(memory, 100, 0xdead)
    }

    @Test fun wordStoreLoadTest() {
        val memory = Memory()
        memory.storeWord(100, 0xdeadbeef.toInt())
        storeAndCheckWord(memory, 100, 0xdeadbeef.toInt())
    }

    @Test fun longStoreLoadTest() {
        val memory = Memory()
        memory.storeLong(100, 0x0eed_daed_dead_beefL)
        storeAndCheckLong(memory, 100, 0x0eed_daed_dead_beefL)
    }

    @Test fun `halfword store load unaligned test`() {
        val mem1 = Memory(alignedAddresses = true)
        storeAndCheckHalfWord(mem1, 100, 0xabcd)
        assertHexEquals(0xabcd, mem1.loadHalfWord(100))
        storeAndCheckHalfWord(mem1, 102, 0xabcd)
        assertFailsWith(AlignmentError::class) { mem1.storeHalfWord(101, 0xffff) }
        assertFailsWith(AlignmentError::class) { mem1.storeHalfWord(103, 0xffff) }
        val mem2 = Memory(alignedAddresses = false)
        storeAndCheckHalfWord(mem2, 101, 0xabcd)
        storeAndCheckHalfWord(mem2, 103, 0xdef0)
    }

    @Test fun `word store load unaligned test`() {
        val mem1 = Memory(alignedAddresses = true)
        storeAndCheckWord(mem1, 100, 0x7bcdefa)
        assertFailsWith(AlignmentError::class) { mem1.storeWord(101, 0xffff) }
        assertFailsWith(AlignmentError::class) { mem1.storeWord(102, 0xffff) }
        assertFailsWith(AlignmentError::class) { mem1.storeWord(103, 0xffff) }
        val mem2 = Memory(alignedAddresses = false)
        storeAndCheckWord(mem2, 101, 0x7bcdefa9)
        storeAndCheckWord(mem2, 102, 0x76543210)
        storeAndCheckWord(mem2, 103, 0x0123abcd)
    }

    @Test fun `long store load unaligned test`() {
        val mem1 = Memory(alignedAddresses = true)
        storeAndCheckLong(mem1, 96, 0x0eed_daed_dead_beefL)
        for (i in 1..7) {
            assertFailsWith(AlignmentError::class) { mem1.storeLong(96 + i, 0xffff) }
        }
        val mem2 = Memory(alignedAddresses = false)
        storeAndCheckLong(mem2, 101, 0x7bcd_efa9_8765_4321L)
        storeAndCheckLong(mem2, 102, 0x7654_3210_1234_5678L)
        storeAndCheckLong(mem2, 103, 0x0123_abcd_ef45_6789L)
        storeAndCheckLong(mem2, 104, 0x0eed_dead_dead_beefL)
        storeAndCheckLong(mem2, 105, 0x1234_5678_9012_3456L)
        storeAndCheckLong(mem2, 106, 0x57aa_aadf_4688_2343L)
        storeAndCheckLong(mem2, 107, 0x3771_2345_5553_bcaaL)
    }

    /**
     * Tests the removeByte function. While the function isn't used by any instructions, it's used on simulator reset.
     */
    @Test fun removeByteTest() {
        val memory = Memory()
        memory.storeWord(100, 0xdeadbeef.toInt())
        memory.removeByte(103)
        assertHexEquals(0x00adbeef, memory.loadWord(100))
        memory.removeByte(101)
        assertHexEquals(0x00ad00ef, memory.loadWord(100))
        memory.removeByte(102)
        assertHexEquals(0x000000ef, memory.loadWord(100))
        memory.removeByte(100)
        assertHexEquals(0x00000000, memory.loadWord(100))
    }
}
