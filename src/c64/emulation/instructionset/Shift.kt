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
        cpu.registerInstruction(0x06, ::opASL)
        cpu.registerInstruction(0x0A, ::opASL)
        cpu.registerInstruction(0x26, ::opROL)
        cpu.registerInstruction(0x2A, ::opROL)
        cpu.registerInstruction(0x46, ::opLSR)
        cpu.registerInstruction(0x4A, ::opLSR)
        cpu.registerInstruction(0x6A, ::opROR)
    }

    /**
     * Rotate Left.
     */
    private fun opROL() {
        // todo: switch for 5 addressing modes...
        var result = 0
        when (cpu.currentOpcode.toInt()) {
            0x26 -> {
                // addressing mode: zeropage
                // cycles: 5
                registers.cycles += 5
                // get zeropage address
                val addr = memory.fetchZeroPageAddressWithPC()
                // shift left by 1
                result = memory.fetch(addr).toInt() shl 1
                // fill bit 0 with the value of the carry flag
                if (registers.C) {
                    result = result or 0x1
                }
                // save bit 8 in the carry flag...
                registers.C = result and 0x100 == 0x100
                // push value back to zeropage address
                memory.push(addr, result.toUByte())
            }
            0x2A -> {
                // addressing mode: accumulator
                // cycles: 2
                registers.cycles += 2
                // shift left by 1
                result = registers.A.toInt() shl 1
                // fill bit 0 with the value of the carry flag
                if (registers.C) {
                    result = result or 0x1
                }
                // save bit 8 in the carry flag...
                registers.C = result and 0x100 == 0x100
                registers.A = result.toUByte()
            }
        }
        registers.setZeroFlagFromValue(result.toUByte())
        registers.setNegativeFlagFromValue(result.toUByte())
    }

    /**
     * Rotate Right.
     */
    private fun opROR() {
        // todo: switch for 5 addressing modes...
        when (cpu.currentOpcode.toInt()) {
            0x6A -> {
                // addressing mode: accumulator
                // cycles: 2
                registers.cycles += 2
                // save carry
                val carry = registers.C
                // move bit 0 in the carry flag...
                registers.C = registers.A.toInt() and 0x01 == 0x01
                // shift right by 1
                var result = registers.A.toInt() shr 1
                // fill bit 7 with the saved value of the carry flag
                if (carry) {
                    result = result or 0b1000_0000
                }
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
        var result = 0
        when (cpu.currentOpcode.toInt()) {
            0x06 -> {
                // addressing mode: zeropage
                // cycles: 5
                registers.cycles += 5
                val addr = memory.fetchZeroPageAddressWithPC()
                // shift left by 1
                result = memory.fetch(addr).toInt() shl 1
                // save bit 8 in the carry flag...
                registers.C = result and 0x100 == 0x100
                // push value back to zeropage address
                memory.push(addr, result.toUByte())
            }
            0x0A -> {
                // addressing mode: accumulator
                // cycles: 2
                registers.cycles += 2
                // shift left by 1
                result = registers.A.toInt() shl 1
                // save bit 8 in the carry flag...
                registers.C = result and 0x100 == 0x100
                registers.A = result.toUByte()
            }
        }
        registers.setZeroFlagFromValue(result.toUByte())
        registers.setNegativeFlagFromValue(result.toUByte())
    }

    /**
     * Logical Shift Right
     */
    private fun opLSR() {
        // todo: switch for 5 addressing modes...
        var result: UByte = 0u
        when (cpu.currentOpcode.toInt()) {
            0x46 -> {
                // addressing mode: zeropage
                // cycles: 5
                registers.cycles += 5
                // get zeropage address
                val addr = memory.fetchZeroPageAddressWithPC()
                // save current value from zeropage
                result = memory.fetch(addr)
                // save bit 0 in the carry flag...
                registers.C = result.toInt() and 0x01 == 0x01
                // shift right by 1
                result = (result.toInt() shr 1).toUByte()
                memory.push(addr, result)
            }
            0x4A -> {
                // addressing mode: accumulator
                // cycles: 2
                registers.cycles += 2
                // save bit 0 in the carry flag...
                registers.C = registers.A.toInt() and 0x01 == 0x01
                // shift right by 1
                result = (registers.A.toInt() shr 1).toUByte()
                registers.A = result
            }
        }
        registers.setZeroFlagFromValue(result)
        // negative flag always 0 after this operation
        registers.N = false
    }
}