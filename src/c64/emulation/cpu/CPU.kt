package c64.emulation.cpu

import c64.emulation.C64ExecutionException
import c64.emulation.Registers
import c64.emulation.cpu.instructionset.*
import c64.emulation.debugger.Debugger
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
    }

    private var disassembly: Disassembly
    private var debugger: Debugger

    // currently executed opcode
    internal var currentOpcode: UByte = 0x00u

    private var numRegisteredOps = 0

    init {
        logger.info { "init CPU 6510/8500" }
        disassembly = Disassembly(registers, memory)
        debugger = Debugger(registers, memory, disassembly)

        disassembly.startDisassemblerAt = 0x0000
        debugger.breakpoint = 0x3469
        debugger.waitForCycle = 96_241_300 //84036048

        // initialize instructions table
        val instructions = arrayOf(::IncrementsDecrements, ::RegisterTransfers, ::LoadStore, ::JumpsCalls,
            ::Arithmetic, ::Logical, ::Branch, ::Stack, ::StatusFlags, ::Shift, ::System)
        instructions.forEach { it(this, registers, memory) }
        logger.debug {"$numRegisteredOps opCodes registered"}
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
        numRegisteredOps++
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
        numRegisteredOps++
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
        numRegisteredOps++
    }

    fun reset() {
        registers.reset()
        registers.PC = memory.fetchWord(RESET_VECTOR)
    }

    fun runMachine() {
        val machineIsRunning = true
        try {
            while (machineIsRunning) {
                // check debugging status, maybe print registers
                debugger.checkStatus()
                // check disassembly status
                disassembly.checkStatus()
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
            if (logger.isDebugEnabled && disassembly.printDisassembledCode) {
                logger.debug(disassembly.disassemble(opcode))
            }
            debugger.handleConsoleDebugging()
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