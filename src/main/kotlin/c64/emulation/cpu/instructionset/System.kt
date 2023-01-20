package c64.emulation.cpu.instructionset

import c64.emulation.System.cpu
import c64.emulation.System.memory
import c64.emulation.System.registers

/**
 * Class collecting all "system" instructions.
 *
 * @author Daniel Schulte 2017-2019
 */
@ExperimentalUnsignedTypes
class System {

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
        // store PC to stack
        memory.pushWordToStack(registers.PC)
        // set break flag
        registers.B = true
        // store processor status to stack
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
        registers.setProcessorStatus(memory.popFromStack())
        // fetch PC from stack
        registers.PC = memory.popWordFromStack()
    }
}