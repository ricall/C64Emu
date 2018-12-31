package c64.emulation

import c64.emulation.cpu.CPU
import c64.emulation.memory.Memory
import mu.KLogging
import org.apache.log4j.BasicConfigurator
import org.apache.log4j.ConsoleAppender
import org.apache.log4j.PatternLayout

/**
 * Bootstrapper for C64 emulator.
 *
 * @author Daniel Schulte 2017-2018
 */
@ExperimentalUnsignedTypes
class BootstrapC64 {

    companion object : KLogging()

    init {
        // PatternLayout("%-5p - %m%n"))
        BasicConfigurator.configure(ConsoleAppender(PatternLayout("%m%n")))
        logger.info { "booting c64 system" }
        val memory = Memory()
        val registers = Registers()
        memory.registers = registers
        val cpu = CPU(registers, memory)
        cpu.reset()

        // TODO: test, see http://visual6502.org/wiki/index.php?title=6502TestPrograms
        //memory.loadIntoRam("./roms/6502_functional_test.bin")
        //registers.PC = 0x0400

        cpu.runMachine()
    }
}

@ExperimentalUnsignedTypes
fun main (args : Array<String>) {
    BootstrapC64()
}