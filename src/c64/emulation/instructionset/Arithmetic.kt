package c64.emulation.instructionset

import c64.emulation.CPU
import c64.emulation.Memory
import c64.emulation.Registers

/**
 * Class collecting all "Arithmetic" instructions.
 *
 * @author schulted 2017-2018
 */
@ExperimentalUnsignedTypes
class Arithmetic(private var cpu: CPU, private var registers: Registers, private var memory: Memory) {

    init {
        cpu.registerInstruction(0x65, ::opADC)
        cpu.registerInstruction(0x69, ::opADC)
        cpu.registerInstruction(0xC4, ::opCPY)
        cpu.registerInstruction(0xC5, ::opCMP)
        cpu.registerInstruction(0xC9, ::opCMP)
        cpu.registerInstruction(0xC0, ::opCPY)
        cpu.registerInstruction(0xC1, ::opCMP)
        cpu.registerInstruction(0xCC, ::opCPY)
        cpu.registerInstruction(0xCD, ::opCMP)
        cpu.registerInstruction(0xD1, ::opCMP)
        cpu.registerInstruction(0xD5, ::opCMP)
        cpu.registerInstruction(0xD9, ::opCMP)
        cpu.registerInstruction(0xDD, ::opCMP)
        cpu.registerInstruction(0xE0, ::opCPX)
        cpu.registerInstruction(0xE4, ::opCPX)
        cpu.registerInstruction(0xE9, ::opSBC)
        cpu.registerInstruction(0xEC, ::opCPX)
    }

    /**
     * Compare memory and accumulator
     * http://www.6502.org/tutorials/compare_beyond.html
     */
    private fun opCMP() {
        var value: UByte = 0u
        when (cpu.currentOpcode.toInt()) {
            0xC1 -> {
                // addressing mode: (indirect,x)
                // cycles: 6
                registers.cycles += 6
                value = memory.fetchIndexedIndirectXWithPC()
            }
            0xC5 -> {
                // addressing mode: zeropage
                // cycles: 3
                registers.cycles += 3
                value = memory.fetchZeroPageWithPC()
            }
            0xC9 -> {
                // addressing mode: immediate
                // cycles: 2
                registers.cycles += 2
                value = memory.fetchWithPC()
            }
            0xCD -> {
                // addressing mode: absolute
                // cycles: 4
                registers.cycles += 4
                value = memory.fetchAbsoluteWithPC()
            }
            0xD1 -> {
                // addressing mode: (indirect), y
                // cycles: 5* (+1 page boundary cross)
                registers.cycles += 5
                value = memory.fetchIndirectIndexedYWithPC()
            }
            0xD5 -> {
                // addressing mode: zeropage, x
                // cycles: 4
                registers.cycles += 4
                value = memory.fetchZeroPageXWithPC()
            }
            0xD9 -> {
                // addressing mode: absolute, y
                // cycles: 4* (+1 page boundary cross)
                registers.cycles += 4
                value = memory.fetchAbsoluteYWithPC()
            }
            0xDD -> {
                // addressing mode: absolute, x
                // cycles: 4* (+1 page boundary cross)
                registers.cycles += 4
                value = memory.fetchAbsoluteXWithPC()
            }
        }
        val compareResult: UByte = (registers.A - value).toUByte()
        registers.setZeroFlagFromValue(compareResult)
        registers.setNegativeFlagFromValue(compareResult)
        registers.C = registers.A >= value
    }

    /**
     * Compare X Register
     */
    private fun opCPX() {
        var value: UByte = 0u
        when (cpu.currentOpcode.toInt()) {
            0xE4 -> {
                // addressing mode: zeropage
                // cycles: 3
                value = memory.fetchZeroPageWithPC()
                registers.cycles += 3
            }
            0xE0 -> {
                // addressing mode: immediate
                // cycles: 2
                value = memory.fetchWithPC()
                registers.cycles += 2
            }
            0xEC -> {
                // addressing mode: absolute
                // cycles: 4
                value = memory.fetchAbsoluteWithPC()
                registers.cycles += 4
            }
        }
        val compareResult: UByte = (registers.X - value).toUByte()
        registers.setZeroFlagFromValue(compareResult)
        registers.setNegativeFlagFromValue(compareResult)
        registers.C = registers.X >= value
    }

    /**
     * Compare X Register
     */
    private fun opCPY() {
        var value: UByte = 0u
        when (cpu.currentOpcode.toInt()) {
            0xC4 -> {
                // addressing mode: zeropage
                // cycles: 3
                value = memory.fetchZeroPageWithPC()
                registers.cycles += 3
            }
            0xC0 -> {
                // addressing mode: immediate
                // cycles: 2
                value = memory.fetchWithPC()
                registers.cycles += 2
            }
            0xCC -> {
                // addressing mode: absolute
                // cycles: 4
                value = memory.fetchAbsoluteWithPC()
                registers.cycles += 4
            }
        }
        val compareResult: UByte = (registers.Y - value).toUByte()
        registers.setZeroFlagFromValue(compareResult)
        registers.setNegativeFlagFromValue(compareResult)
        registers.C = registers.Y >= value
    }

    /**
     * Add with Carry
     */
    private fun opADC() {
        // TODO: add BCD arithmetic if decimal flag is set
        // todo: switch for 8 addressing modes...
        val value: UByte
        when (cpu.currentOpcode.toInt()) {
            0x65 -> {
                // addressing mode: zeropage
                // cycles: 3
                value = memory.fetchZeroPageWithPC()
                registers.cycles += 3
            }
            0x69 -> {
                // addressing mode: immediate
                // cycles: 2
                value = memory.fetchWithPC()
                registers.cycles += 2
            }
            else -> {
                value = 0u
            }
        }
        val carry: UByte = if (registers.C) 1u else 0u
        val result: UInt = registers.A + value + carry
        registers.A = result.toUByte()
        registers.C = result > 0xFFu
        registers.setZeroFlagFromValue(registers.A)
        registers.setNegativeFlagFromValue(registers.A)
        registers.setOverflowFlagFromValue(registers.A)
    }

    /**
     * Subtract with Carry
     */
    private fun opSBC() {
        // TODO: add BCD arithmetic if decimal flag is set
        // todo: switch for 8 addressing modes...
        val value: UByte
        when (cpu.currentOpcode.toInt()) {
            0xE9 -> {
                // addressing mode: immediate
                // cycles: 2
                value = memory.fetchWithPC()
                registers.cycles += 2
            }
            else -> {
                value = 0u
            }
        }
        // TODO - check this method for correctness!!!!!
        val carry: UByte = if (registers.C) 0u else 1u
        val result: UInt = registers.A - value - carry

        // TODO: test test test
        //val a: UByte = 0xFEu
        //val m: UByte = 0xFFu
        //val r:UInt = a - m
        //val carryOn = r > 0xFFu

        registers.A = result.toUByte()
        registers.C = result > 0xFFu
        registers.setZeroFlagFromValue(registers.A)
        registers.setNegativeFlagFromValue(registers.A)
        registers.setOverflowFlagFromValue(registers.A)
    }
}