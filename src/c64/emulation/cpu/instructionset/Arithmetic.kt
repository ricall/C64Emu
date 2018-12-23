package c64.emulation.cpu.instructionset

import c64.emulation.cpu.CPU
import c64.emulation.memory.Memory
import c64.emulation.Registers
import c64.emulation.cpu.AddressingMode

/**
 * Class collecting all "Arithmetic" instructions.
 *
 * @author Daniel Schulte 2017-2018
 */
@ExperimentalUnsignedTypes
class Arithmetic(cpu: CPU, private var registers: Registers, @Suppress("unused") private var memory: Memory) {

    init {
        cpu.registerInstruction(0x61, ::opADC, AddressingMode.IndexedIndirectX, 6)
        cpu.registerInstruction(0x65, ::opADC, AddressingMode.ZeroPage, 3)
        cpu.registerInstruction(0x69, ::opADC, AddressingMode.Immediate, 2)
        cpu.registerInstruction(0x6D, ::opADC, AddressingMode.Absolute, 4)
        cpu.registerInstruction(0x71, ::opADC, AddressingMode.IndirectIndexedY, 5)
        cpu.registerInstruction(0x75, ::opADC, AddressingMode.ZeroPageX, 4)
        cpu.registerInstruction(0x79, ::opADC, AddressingMode.AbsoluteY, 4)
        cpu.registerInstruction(0x7D, ::opADC, AddressingMode.AbsoluteX, 4)
        cpu.registerInstruction(0xC4, ::opCPY, AddressingMode.ZeroPage, 3)
        cpu.registerInstruction(0xC5, ::opCMP, AddressingMode.ZeroPage, 3)
        cpu.registerInstruction(0xC9, ::opCMP, AddressingMode.Immediate, 2)
        cpu.registerInstruction(0xC0, ::opCPY, AddressingMode.Immediate, 2)
        cpu.registerInstruction(0xC1, ::opCMP, AddressingMode.IndexedIndirectX, 6)
        cpu.registerInstruction(0xCC, ::opCPY, AddressingMode.Absolute, 4)
        cpu.registerInstruction(0xCD, ::opCMP, AddressingMode.Absolute, 4)
        cpu.registerInstruction(0xD1, ::opCMP, AddressingMode.IndirectIndexedY, 5)
        cpu.registerInstruction(0xD5, ::opCMP, AddressingMode.ZeroPageX, 4)
        cpu.registerInstruction(0xD9, ::opCMP, AddressingMode.AbsoluteY, 4)
        cpu.registerInstruction(0xDD, ::opCMP, AddressingMode.AbsoluteX, 4)
        cpu.registerInstruction(0xE0, ::opCPX, AddressingMode.Immediate, 2)
        cpu.registerInstruction(0xE1, ::opSBC, AddressingMode.IndexedIndirectX, 6)
        cpu.registerInstruction(0xE4, ::opCPX, AddressingMode.ZeroPage, 3)
        cpu.registerInstruction(0xE5, ::opSBC, AddressingMode.ZeroPage, 3)
        cpu.registerInstruction(0xE9, ::opSBC, AddressingMode.Immediate, 2)
        cpu.registerInstruction(0xEC, ::opCPX, AddressingMode.Absolute, 4)
        cpu.registerInstruction(0xED, ::opSBC, AddressingMode.Absolute, 4)
        cpu.registerInstruction(0xF1, ::opSBC, AddressingMode.IndirectIndexedY, 5)
        cpu.registerInstruction(0xFD, ::opSBC, AddressingMode.AbsoluteX, 4)
        cpu.registerInstruction(0xF5, ::opSBC, AddressingMode.ZeroPageX, 4)
        cpu.registerInstruction(0xF9, ::opSBC, AddressingMode.AbsoluteY, 4)
    }

    /**
     * Compare memory and accumulator
     * http://www.6502.org/tutorials/compare_beyond.html
     */
    private fun opCMP(value: UByte) {
        val compareResult: UByte = (registers.A - value).toUByte()
        registers.setZeroFlagFromValue(compareResult)
        registers.setNegativeFlagFromValue(compareResult)
        registers.C = registers.A >= value
    }

    /**
     * Compare X Register
     */
    private fun opCPX(value: UByte) {
        val compareResult: UByte = (registers.X - value).toUByte()
        registers.setZeroFlagFromValue(compareResult)
        registers.setNegativeFlagFromValue(compareResult)
        registers.C = registers.X >= value
    }

    /**
     * Compare X Register
     */
    private fun opCPY(value: UByte) {
        val compareResult: UByte = (registers.Y - value).toUByte()
        registers.setZeroFlagFromValue(compareResult)
        registers.setNegativeFlagFromValue(compareResult)
        registers.C = registers.Y >= value
    }

    /**
     * Add with Carry
     */
    private fun opADC(value: UByte) {
        // TODO: add BCD arithmetic if decimal flag is set
        val carry: UByte = if (registers.C) 1u else 0u
        val result: UInt = registers.A + value + carry
        val signedResult = registers.A.toByte() + value.toByte() + carry.toByte()
        registers.A = result.toUByte()
        registers.C = result > 0xFFu
        registers.setZeroFlagFromValue(registers.A)
        registers.setNegativeFlagFromValue(registers.A)
        registers.setOverflowFlagFromSignedValue(signedResult)
    }

    /**
     * Subtract with Carry
     */
    private fun opSBC(value: UByte) {
        // TODO: add BCD arithmetic if decimal flag is set
        val carry: UByte = if (registers.C) 0u else 1u
        val result: UInt = registers.A - value - carry
        val signedResult = registers.A.toByte() - value.toByte() - carry.toByte()
        registers.A = result.toUByte()
        registers.C = result <= 0xFFu
        registers.setZeroFlagFromValue(registers.A)
        registers.setNegativeFlagFromValue(registers.A)
        registers.setOverflowFlagFromSignedValue(signedResult)
    }
}