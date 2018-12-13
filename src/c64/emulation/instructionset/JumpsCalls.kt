package c64.emulation.instructionset

import c64.emulation.CPU
import c64.emulation.Memory
import c64.emulation.Registers

/**
 * Class collecting all "Jump / Call" instructions.
 *
 * @author schulted 2017-2018
 */
@ExperimentalUnsignedTypes
class JumpsCalls(private var cpu: CPU, private var registers: Registers, private var memory: Memory) {
    
    init {
        cpu.registerInstruction(0x20, ::opJSR)
        cpu.registerInstruction(0x4C, ::opJMP)
        cpu.registerInstruction(0x60, ::opRTS)
        cpu.registerInstruction(0x6C, ::opJMP)
    }

    /**
     * Jump to Subroutine
     */
    private fun opJSR() {
        // cycles: 6
        val newPC = memory.fetchWord(registers.PC++)
        memory.pushWordToStack(registers.PC)
        registers.PC = newPC
        registers.cycles += 6
    }

    /**
     * Jump to Subroutine
     */
    private fun opJMP() {
        when (cpu.currentOpcode.toInt()) {
            0x4C -> {
                // addressing mode: absolute
                // cycles: 3
                registers.PC = memory.fetchWordWithPC()
                registers.cycles += 3
            }
            0x6C -> {
                // addressing mode: indirect
                // cycles: 5
                registers.PC = memory.fetchWordIndirectWithPC()
                registers.cycles += 5
            }
        }
    }

    /**
     * Return from subroutine
     */
    private fun opRTS() {
        // cycles: 6
        registers.PC = memory.fetchWordFromStack() + 1
        registers.cycles += 6
    }

}