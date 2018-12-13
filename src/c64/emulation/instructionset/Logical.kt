package c64.emulation.instructionset

import c64.emulation.CPU
import c64.emulation.Memory
import c64.emulation.Registers

/**
 * Class collecting all "Logical" instructions.
 *
 * @author schulted 2017-2018
 */
@ExperimentalUnsignedTypes
class Logical(private var cpu: CPU, private var registers: Registers, private var memory: Memory) {

    init {
        cpu.registerInstruction(0x09, ::opORA)
        cpu.registerInstruction(0x0D, ::opORA)
        cpu.registerInstruction(0x24, ::opBIT)
        cpu.registerInstruction(0x2C, ::opBIT)
        cpu.registerInstruction(0x29, ::opAND)
        cpu.registerInstruction(0x49, ::opEOR)
        cpu.registerInstruction(0x55, ::opEOR)
    }

    /**
     * Logical AND
     */
    private fun opAND() {
        // todo: switch for 8 addressing modes...
        when (cpu.currentOpcode.toInt()) {
            0x29 -> {
                // addressing mode: immediate
                // cycles: 2
                registers.cycles += 2
                registers.A = registers.A and memory.fetchWithPC()
            }
            else -> {
            }
        }
        registers.setZeroFlagFromValue(registers.A)
        registers.setNegativeFlagFromValue(registers.A)
    }

    /**
     * Logical Inclusive OR
     */
    private fun opORA() {
        // todo: switch for 8 addressing modes...
        when (cpu.currentOpcode.toInt()) {
            0x09 -> {
                // addressing mode: immediate
                // cycles: 2
                registers.cycles += 2
                registers.A = registers.A or memory.fetchWithPC()
            }
            0x0D -> {
                // addressing mode: absolute
                // cycles: 4
                registers.cycles += 4
                registers.A = registers.A or memory.fetchAbsoluteWithPC()
            }
            else -> {
            }
        }
        registers.setZeroFlagFromValue(registers.A)
        registers.setNegativeFlagFromValue(registers.A)
    }

    /**
     * Exclusive OR
     */
    private fun opEOR() {
        // todo: switch for 8 addressing modes...
        when (cpu.currentOpcode.toInt()) {
            0x49 -> {
                // addressing mode: immediate
                // cycles: 2
                registers.cycles += 2
                registers.A = registers.A xor memory.fetchWithPC()
            }
            0x55 -> {
                // addressing mode: zeropage, x
                // cycles: 4
                registers.cycles += 4
                registers.A = registers.A xor memory.fetchZeroPageXWithPC()
            }
            else -> {
            }
        }
        registers.setZeroFlagFromValue(registers.A)
        registers.setNegativeFlagFromValue(registers.A)
    }

    /**
     * Bit Test
     */
    private fun opBIT() {
        var operand: UByte = 0u
        when (cpu.currentOpcode.toInt()) {
            0x24 -> {
                // addressing mode: zeropage
                // cycles: 3
                registers.cycles += 3
                operand = memory.fetchZeroPageWithPC()
            }
            0x2C -> {
                // addressing mode: absolute
                // cycles: 4
                registers.cycles += 4
                operand = memory.fetchAbsoluteWithPC()
            }
        }
        // overflow flag get's bit 6 of operand
        registers.V = (operand.toInt() and 0b0100_0000) == 0b0100_0000
        // negative flag get's bit 7 of operand
        registers.N = (operand.toInt() and 0b1000_0000) == 0b1000_0000
        // set zero flag from result of accu AND operand
        registers.setZeroFlagFromValue(registers.A and operand)
    }
}