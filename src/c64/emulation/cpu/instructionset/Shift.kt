package c64.emulation.cpu.instructionset

import c64.emulation.cpu.CPU
import c64.emulation.memory.Memory
import c64.emulation.Registers
import c64.emulation.cpu.AddressingMode

/**
 * Class collecting all "Shift" instructions.
 *
 * @author schulted 2017-2018
 */
@ExperimentalUnsignedTypes
class Shift(cpu: CPU, private var registers: Registers, @Suppress("unused") private var memory: Memory) {

    init {
        cpu.registerInstruction(0x06, ::opASL, AddressingMode.ZeroPage, 5)
        cpu.registerInstruction(0x0A, ::opASL, AddressingMode.Accumulator, 2)
        cpu.registerInstruction(0x0E, ::opASL, AddressingMode.Absolute, 6)
        cpu.registerInstruction(0x26, ::opROL, AddressingMode.ZeroPage, 5)
        cpu.registerInstruction(0x2A, ::opROL, AddressingMode.Accumulator, 2)
        cpu.registerInstruction(0x46, ::opLSR, AddressingMode.ZeroPage, 5)
        cpu.registerInstruction(0x4A, ::opLSR, AddressingMode.Accumulator, 2)
        cpu.registerInstruction(0x4E, ::opLSR, AddressingMode.Absolute, 6)
        cpu.registerInstruction(0x66, ::opROR, AddressingMode.ZeroPage, 5)
        cpu.registerInstruction(0x6A, ::opROR, AddressingMode.Accumulator, 2)
    }

    /**
     * Rotate Left.
     */
    private fun opROL(value: UByte): UByte {
        // todo: switch for 5 addressing modes...
        // shift left by 1
        var result = value.toInt() shl 1
        // fill bit 0 with the value of the carry flag
        if (registers.C) {
            result = result or 0x1
        }
        val byteResult = result.toUByte()
        // save bit 8 in the carry flag...
        registers.C = result and 0x100 == 0x100
        registers.setZeroFlagFromValue(byteResult)
        registers.setNegativeFlagFromValue(byteResult)
        return byteResult
    }

    /**
     * Rotate Right.
     */
    private fun opROR(value: UByte): UByte {
        // todo: switch for 5 addressing modes...
        // save carry
        val carry = registers.C
        // move bit 0 in the carry flag...
        registers.C = value.toInt() and 0x01 == 0x01
        // shift right by 1
        var result = value.toInt() shr 1
        // fill bit 7 with the saved value of the carry flag
        if (carry) {
            result = result or 0b1000_0000
        }
        val byteResult = result.toUByte()
        registers.setZeroFlagFromValue(byteResult)
        registers.setNegativeFlagFromValue(byteResult)
        return byteResult
    }

    /**
     * Arithmetic Shift Left.
     */
    private fun opASL(value: UByte): UByte {
        // todo: switch for 5 addressing modes...
        // shift left by 1
        val result: Int = value.toInt() shl 1
        val byteResult = result.toUByte()
        // save bit 8 in the carry flag...
        registers.C = result and 0x100 == 0x100
        registers.setZeroFlagFromValue(byteResult)
        registers.setNegativeFlagFromValue(byteResult)
        return byteResult
    }

    /**
     * Logical Shift Right
     */
    private fun opLSR(value: UByte): UByte {
        // todo: switch for 5 addressing modes...
        // save bit 0 in the carry flag...
        registers.C = value.toInt() and 0x01 == 0x01
        // shift right by 1
        val byteResult = (value.toInt() shr 1).toUByte()
        registers.setZeroFlagFromValue(byteResult)
        // negative flag always 0 after this operation
        registers.N = false
        return byteResult
    }
}