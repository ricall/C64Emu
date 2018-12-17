package c64.emulation.cpu.instructionset

import c64.emulation.cpu.CPU
import c64.emulation.memory.Memory
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
        cpu.registerInstruction(0x0E, ::opASL)
        cpu.registerInstruction(0x26, ::opROL)
        cpu.registerInstruction(0x2A, ::opROL)
        cpu.registerInstruction(0x46, ::opLSR)
        cpu.registerInstruction(0x4A, ::opLSR)
        cpu.registerInstruction(0x4E, ::opLSR)
        cpu.registerInstruction(0x66, ::opROR)
        cpu.registerInstruction(0x6A, ::opROR)
    }

    /**
     * Rotate Left.
     */
    private fun opROL() {
        // todo: switch for 5 addressing modes...
        var value = 0
        var addr = -1
        when (cpu.currentOpcode.toInt()) {
            0x26 -> {
                // addressing mode: zeropage
                // cycles: 5
                registers.cycles += 5
                // get zeropage address
                addr = memory.fetchZeroPageAddressWithPC()
            }
            0x2A -> {
                // addressing mode: accumulator
                // cycles: 2
                registers.cycles += 2
                value = registers.A.toInt()
            }
        }
        if (addr != -1) {
            // get current value from memory
            value = memory.fetch(addr).toInt()
        }
        // shift left by 1
        value = value shl 1
        // fill bit 0 with the value of the carry flag
        if (registers.C) {
            value = value or 0x1
        }
        // save bit 8 in the carry flag...
        registers.C = value and 0x100 == 0x100
        registers.setZeroFlagFromValue(value.toUByte())
        registers.setNegativeFlagFromValue(value.toUByte())

        // write back result
        when (cpu.currentOpcode.toInt()) {
            0x26 -> {
                memory.push(addr, value.toUByte())
            }
            0x2A -> {
                registers.A = value.toUByte()
            }
        }
    }

    /**
     * Rotate Right.
     */
    private fun opROR() {
        // todo: switch for 5 addressing modes...
        var value = 0
        var addr = -1
        when (cpu.currentOpcode.toInt()) {
            0x66 -> {
                // addressing mode: zeropage
                // cycles: 5
                registers.cycles += 5
                addr = memory.fetchZeroPageAddressWithPC()
            }
            0x6A -> {
                // addressing mode: accumulator
                // cycles: 2
                registers.cycles += 2
                value = registers.A.toInt()
            }
        }
        if (addr != -1) {
            // get current value from memory
            value = memory.fetch(addr).toInt()
        }
        // save carry
        val carry = registers.C
        // move bit 0 in the carry flag...
        registers.C = value and 0x01 == 0x01
        // shift right by 1
        value = value shr 1
        // fill bit 7 with the saved value of the carry flag
        if (carry) {
            value = value or 0b1000_0000
        }
        val byteValue = value.toUByte()
        registers.setZeroFlagFromValue(byteValue)
        registers.setNegativeFlagFromValue(byteValue)

        // write back result
        when (cpu.currentOpcode.toInt()) {
            0x66 -> {
                memory.push(addr, byteValue)
            }
            0x6A -> {
                registers.A = byteValue

            }
        }
    }

    /**
     * Arithmetic Shift Left.
     */
    private fun opASL() {
        // todo: switch for 5 addressing modes...
        var value = 0
        var addr = -1
        // get value/address
        when (cpu.currentOpcode.toInt()) {
            0x06 -> {
                // addressing mode: zeropage
                // cycles: 5
                registers.cycles += 5
                addr = memory.fetchZeroPageAddressWithPC()
            }
            0x0A -> {
                // addressing mode: accumulator
                // cycles: 2
                registers.cycles += 2
                value = registers.A.toInt()
            }
            0x0E -> {
                // addressing mode: absolute
                // cycles: 6
                registers.cycles += 6
                addr = memory.fetchWordWithPC()
            }
        }
        if (addr != -1) {
            // get current value from memory
            value = memory.fetch(addr).toInt()
        }
        // shift left by 1
        value = value shl 1
        val byteValue = value.toUByte()
        // save bit 8 in the carry flag...
        registers.C = value and 0x100 == 0x100
        registers.setZeroFlagFromValue(byteValue)
        registers.setNegativeFlagFromValue(byteValue)

        // write back result
        when (cpu.currentOpcode.toInt()) {
            0x06, 0x0E -> {
                // push value back to memory
                memory.push(addr, byteValue)
            }
            0x0A -> {
                registers.A = byteValue
            }
        }
    }

    /**
     * Logical Shift Right
     */
    private fun opLSR() {
        // todo: switch for 5 addressing modes...
        var value: UByte = 0u
        var addr = -1
        // get value/address
        when (cpu.currentOpcode.toInt()) {
            0x46 -> {
                // addressing mode: zeropage
                // cycles: 5
                registers.cycles += 5
                // get zeropage address
                addr = memory.fetchZeroPageAddressWithPC()
            }
            0x4A -> {
                // addressing mode: accumulator
                // cycles: 2
                registers.cycles += 2
                value = registers.A
            }
            0x4E -> {
                // addressing mode: absolute
                // cycles: 6
                registers.cycles += 6
                // get zeropage address
                addr = memory.fetchWordWithPC()
            }
        }
        if (addr != -1) {
            // get current value from memory
            value = memory.fetch(addr)
        }
        // save bit 0 in the carry flag...
        registers.C = value.toInt() and 0x01 == 0x01
        // shift right by 1
        value = (value.toInt() shr 1).toUByte()
        registers.setZeroFlagFromValue(value)
        // negative flag always 0 after this operation
        registers.N = false

        // write back result
        when (cpu.currentOpcode.toInt()) {
            0x46, 0x4E -> {
                memory.push(addr, value)
            }
            0x4A -> {
                registers.A = value
            }
        }
    }

    private fun fetchValueAndIncCycles() {

    }
}