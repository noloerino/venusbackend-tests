/* ktlint-disable package-name */
package venusbackend.simulator
/* ktlint-enable package-name */

import venus.vfs.VirtualFileSystem
import venusbackend.assembler.Assembler
import venusbackend.assertArrayEquals
import venusbackend.linker.Linker
import venusbackend.linker.ProgramAndLibraries
import venusbackend.riscv.Registers
import venusbackend.riscv.insts.integer.base.i.ecall.MallocNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MallocTest {
    val sizeofint = 4
    val NULL = 0
    val start = """
        
        #define sizeofint $sizeofint
        #define NULL 0
        #define true 1
        #define false 0
        
    """.trimIndent()
    val malloc = """
        
        li a0 0x3CC
        li a6 1
        ecall
        
    """.trimIndent()

    val calloc = """
        
        li a0 0x3CC
        li a6 2
        ecall
        
    """.trimIndent()

    val realloc = """
        
        li a0 0x3CC
        li a6 3
        ecall
        
    """.trimIndent()

    val free = """
        
        li a0 0x3CC
        li a6 4
        ecall
        
    """.trimIndent()

    val activeCount = """
        
        li a0 0x3CC
        li a6 5
        ecall
        
    """.trimIndent()

    @Test
    fun simpleMalloc() {
        val (prog, _) = Assembler.assemble("""
        $start
        li a1 sizeofint
        $malloc
        ebreak # Do null check
        li t0 0x162
        sw t0 0(a0)
        mv a1 a0
        $free
        """)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.runToBreakpoint()
        assertNotEquals(0, sim.getReg(Registers.a0))
        sim.run()
        assertEquals(0, sim.alloc.numActiveBlocks())
    }

    @Test
    fun mallocRealloc() {
        // mm_test2
        val text = """
        $start
        li a1 sizeofint
        slli a1 a1 2
        $malloc
        mv s0 a0 # s0 = int *p = malloc(sizeof(int) * 2)
        ebreak # Do null check 1
        
        li a1 sizeofint
        slli a1 a1 2
        $malloc
        mv s1 a0 # s1 = int *q = malloc(sizeof(int) * 2)
        ebreak # Do null check 2
        
        #p[0] = 161
        li t0 161
        sw t0 0(s0)
        
        #p[1] = 162
        li t0 162
        sw t0 4(s0)
        
        #q[0] = 261
        li t0 261
        sw t0 0(s1)
        
        #q[1] = 262
        li t0 262
        sw t0 4(s1)
        
        #s11 = int* oldp = p
        mv s11 s0
        # realloc (p, sizeof(int) * 4) => (s0, sizeof(int) * 4)
        mv a1 s0
        li a2 sizeofint
        li t0 4
        mul a2 a2 t0
        $realloc
        mv s0 a0
        ebreak # Do many checks 1
        
        # int* s = malloc(sizeof(int) * 2)
        # s2 == s
        li a1 sizeofint
        slli a1 a1 2
        $malloc
        mv s2 a0
        ebreak # Do many checks 2
        
        # p = (int*) mm_realloc(p, sizeof(int) * 0xffffffff); # This should fail!
        mv a1 s0 # put p as ptr
        li a2 0xFFFFFFFF
        $realloc
        ebreak # Do too large assert
        
        #Lets finally free everything
        mv a1 s0 # free p
        $free
        
        mv a1 s1 # free q
        $free
        
        mv a1 s2
        $free
        """
        val (prog, _) = Assembler.assemble(text)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.alloc.alwaysCalloc = true
        sim.runToBreakpoint()
        // Do null check 1
        assertNotEquals(NULL, sim.getReg(Registers.a0).toInt())

        sim.runToBreakpoint()
        // Do null check 2
        val a0val = sim.getReg(Registers.a0).toInt()
        assertNotEquals(NULL, a0val)
        assertNotEquals(sim.getReg(Registers.s0).toInt(), a0val)

        sim.runToBreakpoint()
        // Do many checks 1
        var paddr = sim.getReg(Registers.s0).toInt()
        var qaddr = sim.getReg(Registers.s1).toInt()
        assertNotEquals(NULL, paddr) // assert(p != NULL)
        assertTrue(sim.getReg(Registers.s11).toInt() != paddr) // assert(oldp != p)

        assertEquals(161, sim.loadWord(paddr).toInt())
        assertEquals(162, sim.loadWord(paddr + sizeofint).toInt())
        assertEquals(0, sim.loadWord(paddr + sizeofint * 2).toInt())
        assertEquals(0, sim.loadWord(paddr + sizeofint * 3).toInt())
        assertEquals(261, sim.loadWord(qaddr).toInt())
        assertEquals(262, sim.loadWord(qaddr + sizeofint).toInt())

        sim.runToBreakpoint()
        // Do many checks 2
        var saddr = sim.getReg(Registers.s2).toInt()
        assertNotEquals(NULL, saddr) // assert(p != NULL)
        assertEquals(sim.getReg(Registers.s11).toInt(), saddr) // assert(oldp != p)

        assertEquals(0, sim.loadWord(saddr).toInt())
        assertEquals(0, sim.loadWord(saddr + sizeofint).toInt())
        assertEquals(161, sim.loadWord(paddr).toInt())
        assertEquals(162, sim.loadWord(paddr + sizeofint).toInt())
        assertEquals(0, sim.loadWord(paddr + sizeofint * 2).toInt())
        assertEquals(0, sim.loadWord(paddr + sizeofint * 3).toInt())
        assertEquals(261, sim.loadWord(qaddr).toInt())
        assertEquals(262, sim.loadWord(qaddr + sizeofint).toInt())

        sim.runToBreakpoint()
        // Do too large assert
        assertEquals(NULL, sim.getReg(Registers.a0).toInt())

        sim.run()
        assertEquals(0, sim.alloc.numActiveBlocks())
    }

    @Test
    fun bigMalloc() {
        // mm_test3
        val text = """
        $start
        li a1 sizeofint
        slli a1 a1 4
        $malloc
        mv s0 a0 # s0 = int *p = malloc(sizeof(int) * 16)
        ebreak # Do null check 1
        
        li a1 sizeofint
        slli a1 a1 4
        $malloc
        mv s1 a0 # s1 = int *q = malloc(sizeof(int) * 16)
        ebreak # Do null check 2
        
        #p[0] = 161
        li t0 161
        sw t0 0(s0)
        
        #p[1] = 162
        li t0 162
        sw t0 4(s0)
        
        #q[0] = 261
        li t0 261
        sw t0 0(s1)
        
        #q[1] = 262
        li t0 262
        sw t0 4(s1)
        
        # free(p)
        mv a1 s0
        $free
        # s2 = a = malloc (sizeof(int) * 3)
        li a1 sizeofint
        li t0 3
        mul a1 a1 t0
        $malloc
        mv s2 a0
        ebreak # Do new malloc null check 1
        
        # s3 = b = malloc (sizeof(int) * 3)
        li a1 sizeofint
        li t0 3
        mul a1 a1 t0
        $malloc
        mv s3 a0
        ebreak # Do new malloc null check 2 & size check.
        
        # Free q, a, b (remember, we already freed p)
        mv a1 s1
        $free
        mv a1 s2
        $free
        mv a1 s3
        $free
        """
        val (prog, _) = Assembler.assemble(text)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.alloc.alwaysCalloc = true
        sim.runToBreakpoint()
        // Do null check 1
        var a0val = sim.getReg(Registers.a0).toInt()
        assertNotEquals(NULL, a0val)
        print("p = $a0val\n")

        sim.runToBreakpoint()
        // Do null check 2
        a0val = sim.getReg(Registers.a0).toInt()
        print("q = $a0val\n")
        assertNotEquals(NULL, a0val)
        assertNotEquals(sim.getReg(Registers.s0).toInt(), a0val)

        sim.runToBreakpoint()
        // Do new malloc null check 1
        a0val = sim.getReg(Registers.a0).toInt()
        print("a = $a0val\n")
        assertNotEquals(NULL, a0val)

        sim.runToBreakpoint()
        // Do new malloc null check 2 & size check.
        a0val = sim.getReg(Registers.a0).toInt()
        print("b = $a0val\n")
        assertNotEquals(NULL, a0val)
        val qaddr = sim.getReg(Registers.s1).toInt()
        val aaddr = sim.getReg(Registers.s2).toInt()
        val baddr = sim.getReg(Registers.s3).toInt()
        val nodes = MallocNode.nodes
        assertTrue(aaddr < qaddr)
        assertTrue(baddr < qaddr)
        assertEquals(261, sim.loadWord(qaddr).toInt())
        assertEquals(262, sim.loadWord(qaddr + sizeofint).toInt())

        sim.run()
        assertEquals(0, sim.alloc.numActiveBlocks())
    }

    @Test
    fun bigMallocReuse() {
        // mm_test4
        val text = """
        $start
        li a1 sizeofint
        slli a1 a1 4
        addi a1 a1 ${MallocNode.sizeof}
        $malloc
        mv s0 a0 # s0 = int *p = malloc(sizeof(int) * 4 + sizeof(metadata))
        ebreak # Do null check 1
        
        # free(p)
        mv a1 s0
        $free
        
        # s1 = int* q = (int*) malloc(sizeof(int) * 2);
        li a1 sizeofint
        slli a1 a1 1
        $malloc
        mv s1 a0
        ebreak # Do null check 2
        
        # s2 = int* s = (int*) malloc(sizeof(int) * 2);
        li a1 sizeofint
        slli a1 a1 1
        $malloc
        mv s2 a0
        ebreak # Do null check 3 & pointer check
        
        # Free q, s (remember, we already freed p)
        mv a1 s1
        $free
        mv a1 s2
        $free
        """
        val (prog, _) = Assembler.assemble(text)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.alloc.alwaysCalloc = true
        sim.runToBreakpoint()
        // Do null check 1
        var a0val = sim.getReg(Registers.a0).toInt()
        assertNotEquals(NULL, a0val)

        sim.runToBreakpoint()
        // Do null check 2
        a0val = sim.getReg(Registers.a0).toInt()
        assertNotEquals(NULL, a0val)
        assertEquals(sim.getReg(Registers.s0).toInt(), a0val)

        sim.runToBreakpoint()
        // Do null check 3 & pointer check
        a0val = sim.getReg(Registers.a0).toInt()
        assertNotEquals(NULL, a0val)

        val paddr = sim.getReg(Registers.s0).toInt()
        val qaddr = sim.getReg(Registers.s1).toInt()
        val saddr = sim.getReg(Registers.s2).toInt()
        assertEquals(paddr, qaddr)
        assertEquals(qaddr + sizeofint * 2 + MallocNode.sizeof, saddr)

        sim.run()
        assertEquals(0, sim.alloc.numActiveBlocks())
    }

    @Test
    fun addDataToMalloc() {
        // mm_test5
        val text = """
        $start
        li a1 ${sizeofint * 5}
        $malloc
        mv s0 a0 # s0 = int *a = malloc(sizeof(int) * 5)
        ebreak # Do null check 1
        
        # s1 = int* b = (int*) malloc(sizeof(int) * 5);
        li a1 ${sizeofint * 5}
        $malloc
        mv s1 a0
        ebreak # Do null check 2
        
        # Loop which adds 25 + i to each item in b
        li t0 5
        li t1 0
        mv t3 s1
    loop:
        addi t4 t1 25
        sw t4 0(t3)
        addi t3 t3 $sizeofint
        addi t1 t1 1
        bne t0 t1 loop
        
        # free(a)
        mv a1 s0
        $free
        
        # s2 = int* c = (int*) malloc(sizeof(int) * 30);
        li a1 ${sizeofint * 30}
        $malloc
        mv s2 a0
        ebreak # Do null check 3 & b validate
        
        # c = realloc(c, sizeof(int) * 5)
        mv a1 s2
        li a2 ${sizeofint * 5}
        $realloc
        mv s2 a0
        ebreak # Do null check 4 & b validate
        
        
        # Free b, c (remember, we already freed a)
        mv a1 s1
        $free
        mv a1 s2
        $free
        """
        val (prog, _) = Assembler.assemble(text)
        val PandL = ProgramAndLibraries(listOf(prog), VirtualFileSystem("dummy"))
        val linked = Linker.link(PandL)
        val sim = Simulator(linked)
        sim.alloc.alwaysCalloc = true
        sim.runToBreakpoint()
        // Do null check 1
        var a0val = sim.getReg(Registers.a0).toInt()
        assertNotEquals(NULL, a0val)

        sim.runToBreakpoint()
        // Do null check 2
        a0val = sim.getReg(Registers.a0).toInt()
        assertNotEquals(NULL, a0val)

        sim.runToBreakpoint()
        // # Do null check 3 & b validate
        a0val = sim.getReg(Registers.a0).toInt()
        assertNotEquals(NULL, a0val)

        // Check b fn:
        val checkb = { ->
            val baddr = sim.getReg(Registers.s1).toInt()
            val expected = arrayListOf<Int>()
            val actual = arrayListOf<Int>()
            for (i in 0 until 5) {
                expected.add(i + 25)
                actual.add(sim.loadWord(baddr + (i * sizeofint)).toInt())
            }
            assertArrayEquals(expected, actual)
        }
        checkb()

        sim.runToBreakpoint()
        // # Do null check 4 & b validate
        a0val = sim.getReg(Registers.a0).toInt()
        assertNotEquals(NULL, a0val)
        checkb()

        sim.run()
        assertEquals(0, sim.alloc.numActiveBlocks())
    }
}