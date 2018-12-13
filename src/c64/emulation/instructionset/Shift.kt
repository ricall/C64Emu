package c64.emulation.instructionset

import c64.emulation.CPU
import c64.emulation.Memory
import c64.emulation.Registers

/**
 * Class collecting all "Shift" instructions.
 *
 * @author schulted 2017-2018
 */
@ExperimentalUnsignedTypes
class Shift(private var cpu: CPU, private var registers: Registers, @Suppress("unused") private var memory: Memory) {

    init {
        cpu.registerInstruction(0x0A, ::opASL)
        cpu.registerInstruction(0x2A, ::opROL)
        cpu.registerInstruction(0x4A, ::opLSR)
    }

    /**
     * Rotate Left.
     */
    private fun opROL() {
        // todo: switch for 5 addressing modes...
        when (cpu.currentOpcode.toInt()) {
            0x2A -> {
                // addressing mode: accumulator
                // cycles: 2
                registers.cycles += 2
                // shift left by 1
                var result = registers.A.toInt() shl 1
                // fill bit 0 with the value of the carry flag
                if (registers.C) {
                    result = result or 0x1
                }
                // save bit 8 in the carry flag...
                registers.C = result and 0x100 == 0x100
                registers.A = result.toUByte()
            }
        }
        registers.setZeroFlagFromValue(registers.A)
        registers.setNegativeFlagFromValue(registers.A)
    }

    /**
     * Arithmetic Shift Left.
     */
    private fun opASL() {
        // todo: switch for 5 addressing modes...
        when (cpu.currentOpcode.toInt()) {
            0x0A -> {
                // addressing mode: accumulator
                // cycles: 2
                registers.cycles += 2
                // shift left by 1
                val result = registers.A.toInt() shl 1
                // save bit 8 in the carry flag...
                registers.C = result and 0x100 == 0x100
                registers.A = result.toUByte()
            }
        }
        registers.setZeroFlagFromValue(registers.A)
        registers.setNegativeFlagFromValue(registers.A)
    }

    /**
     * Logical Shift Right
     */
    private fun opLSR() {
        // todo: switch for 5 addressing modes...
        when (cpu.currentOpcode.toInt()) {
            0x4A -> {
                // addressing mode: accumulator
                // cycles: 2
                registers.cycles += 2
                // save bit 0 in the carry flag...
                registers.C = registers.A.toInt() and 0x01 == 0x01
                // shift right by 1
                registers.A = (registers.A.toInt() shr 1).toUByte()
            }
        }
        registers.setZeroFlagFromValue(registers.A)
        // negative flag always 0 after this operation
        registers.N = false
    }
}