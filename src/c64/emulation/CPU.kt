package c64.emulation

import c64.emulation.disassemble.Disassembly
import c64.emulation.instructionset.*
import c64.emulation.instructionset.Stack
import c64.util.toHex
import mu.KotlinLogging
import java.util.*

private val logger = KotlinLogging.logger {}

// alias for "Instruction" functions
typealias Instruction = () -> Unit

/**
 * Emulator for CPU MOS 6510/8500.
 *
 * @author schulted 2017-2018
 */
@ExperimentalUnsignedTypes
class CPU(private var registers: Registers, private var memory: Memory) {

    companion object {
        const val RESET_VECTOR: Int = 0xFFFC
        // table with all instructions methods indexed with their opcode
        val INSTRUCTION_TABLE = arrayOfNulls<Instruction>(0x100)
    }

    private var disassembly: Disassembly

    // currently executed opcode
    internal var currentOpcode: UByte = 0x00u

    // should the executed code printed as disassembly
    private var printDisassembledCode: Boolean = false
    // debug mode after breakpoint reached
    private var debugging = false

    private val scanner = Scanner(java.lang.System.`in`)

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
        INSTRUCTION_TABLE[opCode] = instruction
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

        // kernel entry point: $FCE2
        //val breakpoint = 0xFF5E
        val breakpoint = 0x0000
        //val startDisassembleAt = 0xB6E1  // 0xFCE2
        val startDisassembleAt = 0x25CF

        val machineIsRunning = true
        try {
            while (machineIsRunning) {
                if (registers.PC == breakpoint) {
                    debugging = true
                    printDisassembledCode = true
                }
                if (debugging) {
                    logger.debug { registers.printRegisters() }
                    logger.debug { memory.printStackLine() }
                }
                // check whether disassembly should be printed
                printDisassembledCode = printDisassembledCode || registers.PC == startDisassembleAt
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
        val command = INSTRUCTION_TABLE[opcode.toInt()]
        if (command != null) {
            // print disassembled code
            if (logger.isDebugEnabled && printDisassembledCode) {
                logger.debug(disassembly.disassemble(opcode))
            }
            if (debugging) {
                scanner.nextLine()
            }
            command.invoke()
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
}