package c64.emulation.instructionset

import c64.emulation.CPU
import c64.emulation.Memory
import c64.emulation.Registers

/**
 * Class collecting all "Stack" instructions.
 *
 * @author schulted 2017-2018
 */
@ExperimentalUnsignedTypes
class Stack(cpu: CPU, private var registers: Registers, @Suppress("unused") private var memory: Memory) {

    init {
        cpu.registerInstruction(0x08, ::opPHP)
        cpu.registerInstruction(0x28, ::opPLP)
        cpu.registerInstruction(0x48, ::opPHA)
        cpu.registerInstruction(0x68, ::opPLA)
        cpu.registerInstruction(0x9A, ::opTXS)
        cpu.registerInstruction(0xBA, ::opTSX)
    }

    /**
     * Push processor status on stack.
     */
    private fun opPHP() {
        // cycles: 3
        memory.pushToStack(registers.getProcessorStatus())
        registers.cycles += 3
    }

    /**
     * Pull processor status from stack
     */
    private fun opPLP() {
        // cycles: 4
        registers.setProcessorStatus(memory.fetchFromStack())
        registers.cycles += 4
    }

    /**
     * Push accumulator on stack.
     */
    private fun opPHA() {
        // cycles: 3
        memory.pushToStack(registers.A)
        registers.cycles += 3
    }

    /**
     * Pull accumulator from stack.
     */
    private fun opPLA() {
        // cycles: 4
        registers.A = memory.fetchFromStack()
        registers.cycles += 4
        registers.setZeroFlagFromValue(registers.A)
        registers.setNegativeFlagFromValue(registers.A)
    }

    /**
     * Transfer index X to stack pointer.
     */
    private fun opTXS() {
        // cycles: 2
        registers.SP = registers.X
        registers.cycles += 2
    }

    /**
     * Transfer stack pointer to X.
     */
    private fun opTSX() {
        // cycles: 2
        registers.X = registers.SP
        registers.cycles += 2
        registers.setZeroFlagFromValue(registers.X)
        registers.setNegativeFlagFromValue(registers.X)
    }
}