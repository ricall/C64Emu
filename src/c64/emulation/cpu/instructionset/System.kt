package c64.emulation.cpu.instructionset

import c64.emulation.cpu.CPU
import c64.emulation.memory.Memory
import c64.emulation.Registers

/**
 * Class collecting all "system" instructions.
 *
 * @author schulted 2017-2018
 */
@ExperimentalUnsignedTypes
class System(cpu: CPU, private var registers: Registers, @Suppress("unused") private var memory: Memory) {

    init {
        cpu.registerInstruction(0x00, ::opBRK)
        cpu.registerInstruction(0x40, ::opRTI)
        cpu.registerInstruction(0xEA, ::opNOP)
    }

    /**
     * No Operation.
     */
    private fun opNOP() {
        // cycles: 2
        registers.cycles += 2
    }

    /**
     * Force an interrupt.
     */
    private fun opBRK() {
        // cycles: 7
        registers.cycles += 7
        // increment PC - byte after opCode will be ignored
        registers.PC++
        // push PC to stack
        memory.pushWordToStack(registers.PC)
        // set break flag
        registers.B = true
        // push processor status to stack
        memory.pushToStack(registers.getProcessorStatus())
        // set interrupt flag
        registers.I = true
        // load IRQ/BRK vector into PC
        registers.PC = memory.fetchWord(0xFFFE)
    }

    /**
     * Return from Interrupt.
     */
    private fun opRTI() {
        // cycles: 6
        registers.cycles += 6
        // fetch processor status from stack and set all flags
        registers.setProcessorStatus(memory.fetchFromStack())
        // fetch PC from stack
        registers.PC = memory.fetchWordFromStack()
    }
}