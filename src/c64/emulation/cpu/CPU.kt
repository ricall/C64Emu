package c64.emulation.cpu

import c64.emulation.C64ExecutionException
import c64.emulation.Registers
import c64.emulation.cpu.instructionset.*
import c64.emulation.disassemble.Disassembly
import c64.emulation.memory.Memory
import c64.util.toHex
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

// alias for "Instruction" functions
typealias Instruction = () -> Unit
@ExperimentalUnsignedTypes
typealias InstructionWithArgAndResult = (value: UByte) -> UByte
@ExperimentalUnsignedTypes
typealias InstructionWithArg = (value: UByte) -> Unit

/**
 * Emulator for CPU MOS 6510/8500.
 *
 * @author Daniel Schulte 2017-2018
 */
@ExperimentalUnsignedTypes
class CPU(private var registers: Registers, private var memory: Memory) {

    private data class OpCodeInfo(val instruction: Instruction)
    private data class OpCodeInfoWithArg(
        val instruction: InstructionWithArg,
        val addressingMode: AddressingMode,
        val cycles: Int
    )
    private data class OpCodeInfoWithArgAndResult(
        val instruction: InstructionWithArgAndResult,
        val addressingMode: AddressingMode,
        val cycles: Int
    )

    companion object {
        const val RESET_VECTOR: Int = 0xFFFC
        // table with all instructions methods indexed with their opcode
        private val INSTRUCTION_TABLE = arrayOfNulls<Any>(0x100)
        // kernel entry point: $FCE2
        private const val BREAKPOINT = 0x0000
        private const val START_DISASSEMBLER_AT = 0x308F
    }

    private var disassembly: Disassembly

    // currently executed opcode
    internal var currentOpcode: UByte = 0x00u

    // should the executed code printed as disassembly
    private var printDisassembledCode: Boolean = false
    // debug mode after breakpoint reached
    private var debugging = false

    private var numOps = 0

    init {
        logger.info { "init CPU 6510/8500" }
        disassembly = Disassembly(registers, memory)

        // initialize instructions table
        val instructions = arrayOf(::IncrementsDecrements, ::RegisterTransfers, ::LoadStore, ::JumpsCalls,
            ::Arithmetic, ::Logical, ::Branch, ::Stack, ::StatusFlags, ::Shift, ::System)
        instructions.forEach { it(this, registers, memory) }
        logger.debug {"$numOps opCodes registered"}
    }

    /**
     * Registers a given instruction with the given opCode.
     */
    internal fun registerInstruction(opCode: Int, instruction: Instruction) {
        // check for duplicate entries
        if (INSTRUCTION_TABLE[opCode] != null) {
            throw IllegalArgumentException("Duplicate registration of opcode <${opCode.toUByte().toHex()}> !")
        }
        INSTRUCTION_TABLE[opCode] = OpCodeInfo(instruction)
        numOps++
    }

    /**
     * Registers a given instruction (with args but without result) with the given opCode, addressingMode and cycles.
     */
    internal fun registerInstruction(opCode: Int, instruction: InstructionWithArg, addressingMode: AddressingMode,
                                     cycles: Int) {
        // check for duplicate entries
        if (INSTRUCTION_TABLE[opCode] != null) {
            throw IllegalArgumentException("Duplicate registration of opcode <${opCode.toUByte().toHex()}> !")
        }
        INSTRUCTION_TABLE[opCode] = OpCodeInfoWithArg(instruction, addressingMode, cycles)
        numOps++
    }

    /**
     * Registers a given instruction (with args and result) with the given opCode, addressingMode and cycles.
     */
    internal fun registerInstructionWithResult(opCode: Int, instruction: InstructionWithArgAndResult,
                                               addressingMode: AddressingMode, cycles: Int) {
        // check for duplicate entries
        if (INSTRUCTION_TABLE[opCode] != null) {
            throw IllegalArgumentException("Duplicate registration of opcode <${opCode.toUByte().toHex()}> !")
        }
        INSTRUCTION_TABLE[opCode] = OpCodeInfoWithArgAndResult(instruction, addressingMode, cycles)
        numOps++
    }

    fun reset() {
        registers.reset()
        registers.PC = memory.fetchWord(RESET_VECTOR)
    }

    fun runMachine() {

        // http://unusedino.de/ec64/technical/aay/c64/krnromma.htm
        // https://www.c64-wiki.de/wiki/%C3%9Cbersicht_6502-Assemblerbefehle
        // http://www.obelisk.me.uk/6502/instructions.html

        val machineIsRunning = true
        try {
            while (machineIsRunning) {
                if (registers.PC == BREAKPOINT) {
                    debugging = true
                    printDisassembledCode = true
                }
                if (debugging) {
                    logger.debug { registers.printRegisters() }
                }
                // check whether disassembly should be printed
                printDisassembledCode = printDisassembledCode || registers.PC == START_DISASSEMBLER_AT
                // fetch byte from memory
                currentOpcode = memory.fetchWithPC()
                // decode and run opcode
                decodeAndRunOpCode(currentOpcode)

                // todo: handle cycles?
            }
        } catch (ex: C64ExecutionException) {
            logger.error { ex.message }
        }
    }

    @Throws(C64ExecutionException::class)
    private fun decodeAndRunOpCode(opcode: UByte) {
        val opCodeInfo = INSTRUCTION_TABLE[opcode.toInt()]
        if (opCodeInfo != null) {
            // print disassembled code
            if (logger.isDebugEnabled && printDisassembledCode) {
                logger.debug(disassembly.disassemble(opcode))
            }
            if (debugging) {
                disassembly.handleConsoleDebugging()
            }
            if (opCodeInfo is OpCodeInfo) {
                opCodeInfo.instruction.invoke()
            }
            else if (opCodeInfo is OpCodeInfoWithArg) {
                runOpcodeWithFetch(opCodeInfo)
            }
            else if (opCodeInfo is OpCodeInfoWithArgAndResult) {
                runOpcodeWithFetchStore(opCodeInfo)
            }
        }
        else {
            // reset PC to get the correct register output
            registers.PC--
            throw C64ExecutionException(
                "CPU jam! Found unknown op-code <${opcode.toHex()}>\n" +
                        "${registers.printRegisters()}\n" +
                        memory.printMemoryLineWithAddress(registers.PC)
            )
        }
    }

    private fun runOpcodeWithFetch(opCodeInfo: OpCodeInfoWithArg) {
        val value: UByte
        // fetch value from memory
        when (opCodeInfo.addressingMode) {
            AddressingMode.ZeroPage -> {
                value = memory.fetchZeroPageWithPC()
            }
            AddressingMode.ZeroPageX -> {
                value = memory.fetchZeroPageXWithPC()
            }
            AddressingMode.Accumulator -> {
                value = registers.A
            }
            AddressingMode.Immediate -> {
                value = memory.fetchWithPC()
            }
            AddressingMode.Absolute -> {
                value = memory.fetchAbsoluteWithPC()
            }
            AddressingMode.AbsoluteX -> {
                value = memory.fetchAbsoluteXWithPC()
            }
            AddressingMode.AbsoluteY -> {
                value = memory.fetchAbsoluteYWithPC()
            }
            AddressingMode.IndexedIndirectX -> {
                value = memory.fetchIndexedIndirectXWithPC()
            }
            AddressingMode.IndirectIndexedY -> {
                value = memory.fetchIndirectIndexedYWithPC()
            }
            else -> {
                value = 0u
            }
        }
        opCodeInfo.instruction.invoke(value)
        // increment cycles
        registers.cycles += opCodeInfo.cycles
    }

    private fun runOpcodeWithFetchStore(opCodeInfo: OpCodeInfoWithArgAndResult) {
        var value: UByte = 0u
        var addr = -1
        // fetch value from memory
        when (opCodeInfo.addressingMode) {
            AddressingMode.ZeroPage -> {
                addr = memory.fetchZeroPageAddressWithPC()
            }
            AddressingMode.ZeroPageX -> {
                addr = memory.fetchZeroPageXAddressWithPC()
            }
            AddressingMode.Accumulator -> {
                value = registers.A
            }
            AddressingMode.Absolute -> {
                addr = memory.fetchWordWithPC()
            }
            AddressingMode.AbsoluteX -> {
                addr = memory.fetchWordWithPC() + registers.X.toInt()
            }
            AddressingMode.AbsoluteY -> {
                addr = memory.fetchWordWithPC() + registers.Y.toInt()
            }
            else -> {
                addr = 0
                value = 0u
            }
        }
        if (addr != -1) {
            value = memory.fetch(addr)
        }
        value = opCodeInfo.instruction.invoke(value)
        // increment cycles
        registers.cycles += opCodeInfo.cycles
        // write back value to memory
        when (opCodeInfo.addressingMode) {
            AddressingMode.ZeroPage,
            AddressingMode.ZeroPageX,
            AddressingMode.Absolute,
            AddressingMode.AbsoluteX,
            AddressingMode.AbsoluteY -> {
                memory.store(addr, value)
            }
            AddressingMode.Accumulator -> {
                registers.A = value
            }
            else -> {

            }
        }
    }
}