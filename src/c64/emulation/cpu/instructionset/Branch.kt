package c64.emulation.cpu.instructionset

import c64.emulation.cpu.CPU
import c64.emulation.memory.Memory
import c64.emulation.memory.Memory.Companion.checkForPageBoundaryCross
import c64.emulation.Registers

/**
 * Class collecting all "Branch" instructions.
 *
 * @author Daniel Schulte 2017-2018
 */
@ExperimentalUnsignedTypes
class Branch(cpu: CPU, private var registers: Registers, private var memory: Memory) {

    init {
        cpu.registerInstruction(0x10, ::opBPL)
        cpu.registerInstruction(0x30, ::opBMI)
        cpu.registerInstruction(0x50, ::opBVC)
        cpu.registerInstruction(0x70, ::opBVS)
        cpu.registerInstruction(0x90, ::opBCC)
        cpu.registerInstruction(0xB0, ::opBCS)
        cpu.registerInstruction(0xD0, ::opBNE)
        cpu.registerInstruction(0xF0, ::opBEQ)
    }

    // TODO - move common branching code into one method
    /**
     * Branch if Not Equal (zero flag not set).
     */
    private fun opBNE() {
        // cycles: 2 (+1 if branch succeeds, +2 if to a new page)
        registers.cycles += 2
        // addressing mode: relative
        if (!registers.Z) {
            registers.cycles++
            // attention: relative offset to the current PC is SIGNED!
            val relativeOffset = memory.fetchWithPC().toByte()
            val branchAddress = registers.PC + relativeOffset
            registers.cycles += checkForPageBoundaryCross(registers.PC, branchAddress)
            registers.PC = branchAddress
        } else {
            registers.PC++
        }
    }

    /**
     * Branch if equal (zero flag set).
     */
    private fun opBEQ() {
        // cycles: 2 (+1 if branch succeeds, +2 if to a new page)
        registers.cycles += 2
        // addressing mode: relative
        if (registers.Z) {
            registers.cycles++
            // attention: relative offset to the current PC is SIGNED!
            val relativeOffset = memory.fetchWithPC().toByte()
            val branchAddress = registers.PC + relativeOffset
            registers.cycles += checkForPageBoundaryCross(registers.PC, branchAddress)
            registers.PC = branchAddress
        } else {
            registers.PC++
        }
    }

    /**
     * Branch if Carry Set.
     */
    private fun opBCS() {
        // cycles: 2 (+1 if branch succeeds, +2 if to a new page)
        registers.cycles += 2
        // addressing mode: relative
        if (registers.C) {
            registers.cycles++
            // attention: relative offset to the current PC is SIGNED!
            val relativeOffset = memory.fetchWithPC().toByte()
            val branchAddress = registers.PC + relativeOffset
            registers.cycles += checkForPageBoundaryCross(registers.PC, branchAddress)
            registers.PC = branchAddress
        } else {
            registers.PC++
        }
    }

    /**
     * Branch if Carry Clear.
     */
    private fun opBCC() {
        // cycles: 2 (+1 if branch succeeds, +2 if to a new page)
        registers.cycles += 2
        // addressing mode: relative
        if (!registers.C) {
            registers.cycles++
            // attention: relative offset to the current PC is SIGNED!
            val relativeOffset = memory.fetchWithPC().toByte()
            val branchAddress = registers.PC + relativeOffset
            registers.cycles += checkForPageBoundaryCross(registers.PC, branchAddress)
            registers.PC = branchAddress
        } else {
            registers.PC++
        }
    }

    /**
     * Branch if Positive (negative flag not set).
     */
    private fun opBPL() {
        // cycles: 2 (+1 if branch succeeds, +2 if to a new page)
        registers.cycles += 2
        // addressing mode: relative
        if (!registers.N) {
            registers.cycles++
            // attention: relative offset to the current PC is SIGNED!
            val relativeOffset = memory.fetchWithPC().toByte()
            val branchAddress = registers.PC + relativeOffset
            registers.cycles += checkForPageBoundaryCross(registers.PC, branchAddress)
            registers.PC = branchAddress
        } else {
            registers.PC++
        }
    }

    /**
     * Branch if Minus (negative flag is set).
     */
    private fun opBMI() {
        // cycles: 2 (+1 if branch succeeds, +2 if to a new page)
        registers.cycles += 2
        // addressing mode: relative
        if (registers.N) {
            registers.cycles++
            // attention: relative offset to the current PC is SIGNED!
            val relativeOffset = memory.fetchWithPC().toByte()
            val branchAddress = registers.PC + relativeOffset
            registers.cycles += checkForPageBoundaryCross(registers.PC, branchAddress)
            registers.PC = branchAddress
        } else {
            registers.PC++
        }
    }

    /**
     * Branch if overflow flag clear.
     */
    private fun opBVC() {
        // cycles: 2 (+1 if branch succeeds, +2 if to a new page)
        registers.cycles += 2
        // addressing mode: relative
        if (!registers.V) {
            registers.cycles++
            // attention: relative offset to the current PC is SIGNED!
            val relativeOffset = memory.fetchWithPC().toByte()
            val branchAddress = registers.PC + relativeOffset
            registers.cycles += checkForPageBoundaryCross(registers.PC, branchAddress)
            registers.PC = branchAddress
        } else {
            registers.PC++
        }
    }

    /**
     * Branch if overflow flag set.
     */
    private fun opBVS() {
        // cycles: 2 (+1 if branch succeeds, +2 if to a new page)
        registers.cycles += 2
        // addressing mode: relative
        if (registers.V) {
            registers.cycles++
            // attention: relative offset to the current PC is SIGNED!
            val relativeOffset = memory.fetchWithPC().toByte()
            val branchAddress = registers.PC + relativeOffset
            registers.cycles += checkForPageBoundaryCross(registers.PC, branchAddress)
            registers.PC = branchAddress
        } else {
            registers.PC++
        }
    }
}