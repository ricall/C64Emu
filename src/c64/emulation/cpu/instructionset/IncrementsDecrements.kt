package c64.emulation.cpu.instructionset

import c64.emulation.cpu.CPU
import c64.emulation.memory.Memory
import c64.emulation.Registers

/**
 * Class collecting all "Increment / Decrement" instructions.
 *
 * @author schulted 2017-2018
 */
@ExperimentalUnsignedTypes
class IncrementsDecrements(private var cpu: CPU, private var registers: Registers, private var memory: Memory) {

    init {
        cpu.registerInstruction(0x88, ::opDEY)
        cpu.registerInstruction(0xC8, ::opINY)
        cpu.registerInstruction(0xCA, ::opDEX)
        cpu.registerInstruction(0xE6, ::opINC)
        cpu.registerInstruction(0xE8, ::opINX)
    }

    /**
     * Increment memory
     */
    private fun opINC() {
        // todo: switch for 4 addressing modes...
        when (cpu.currentOpcode.toInt()) {
            0xE6 -> {
                // addressing mode: zeropage
                // cycles: 5
                val addr = memory.fetchWithPC().toInt()
                memory.push(addr, (memory.fetch(addr) + 1u).toUByte())
                registers.cycles += 5
            }
            else -> {
            }
        }
        registers.setZeroFlagFromValue(registers.Y)
        registers.setNegativeFlagFromValue(registers.Y)
    }

    /**
     * Increment X Register
     */
    private fun opINX() {
        // cycles: 2
        registers.X++
        registers.cycles += 2
        registers.setZeroFlagFromValue(registers.X)
        registers.setNegativeFlagFromValue(registers.X)
    }

    /**
     * Increment Y Register
     */
    private fun opINY() {
        // cycles: 2
        registers.Y++
        registers.cycles += 2
        registers.setZeroFlagFromValue(registers.Y)
        registers.setNegativeFlagFromValue(registers.Y)
    }

    /**
     * Decrement the X register
     */
    private fun opDEX() {
        // cycles: 2
        registers.X--
        registers.cycles += 2
        registers.setZeroFlagFromValue(registers.X)
        registers.setNegativeFlagFromValue(registers.X)
    }

    /**
     * Decrement the Y register
     */
    private fun opDEY() {
        // cycles: 2
        registers.Y--
        registers.cycles += 2
        registers.setZeroFlagFromValue(registers.Y)
        registers.setNegativeFlagFromValue(registers.Y)
    }
}