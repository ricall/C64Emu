package c64.emulation.debugger

import c64.emulation.System.memory
import c64.emulation.System.registers
import c64.emulation.disassemble.Disassembly
import mu.KotlinLogging
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 * Class which handles debugging
 *
 * @author schulted
 */
@ExperimentalUnsignedTypes
class Debugger(private var disassembly: Disassembly) {

    companion object {
        private val PRINT_MEM_CMD = Regex("^m[0-9a-f]{4}$")
        private val PRINT_STACK_CMD = Regex("^s$")
        private val CONTINUE_RUN = Regex("^c$")
        private val RUN_NUM_CYCLES = Regex("^cy[0-9]+$")
    }

    // kernel entry point: $FCE2
    var breakpoint: Int = 0
    // debug mode after breakpoint reached
    var debugging = false
    // start debugging at cycle
    var waitForCycle: Int = -1

    private val scanner = Scanner(System.`in`)

    fun checkStatus() {
        if (!debugging &&
            (registers.PC == breakpoint || (waitForCycle != -1 && registers.cycles >= waitForCycle))
        ) {
            debugging = true
            waitForCycle = -1
            disassembly.printDisassembledCode = true
        }
        if (debugging) {
            logger.debug { registers.printRegisters() }
        }
    }

    fun handleConsoleDebugging() {
        if (!debugging) {
            return
        }
        var continueRun = false
        while (!continueRun) {
            val consoleInput = scanner.nextLine()
            when {
                consoleInput.matches(PRINT_MEM_CMD) -> {
                    val addr = consoleInput.substring(1).toInt(16)
                    logger.debug { memory.printMemoryLineWithAddress(addr) }
                }
                consoleInput.matches(PRINT_STACK_CMD) -> {
                    logger.debug { memory.printStackLine() }
                }
                consoleInput.matches(RUN_NUM_CYCLES) -> {
                    val cycles = consoleInput.substring(2).toInt()
                    waitForCycle = registers.cycles + cycles
                    debugging = false
                    continueRun = true
                }
                consoleInput.matches(CONTINUE_RUN) -> {
                    // disable debugging and continue "normal" run
                    debugging = false
                    continueRun = true
                }
                else -> continueRun = true
            }
        }
    }


}