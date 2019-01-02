package c64.emulation

import mu.KLogging
import org.apache.log4j.BasicConfigurator
import org.apache.log4j.ConsoleAppender
import org.apache.log4j.PatternLayout

/**
 * Bootstrapper for C64 emulator.
 *
 * @author Daniel Schulte 2017-2019
 */
@ExperimentalUnsignedTypes
class BootstrapC64 {

    companion object : KLogging()

    init {
        // PatternLayout("%-5p - %m%n"))
        BasicConfigurator.configure(ConsoleAppender(PatternLayout("%m%n")))
        logger.info { "booting c64 system" }
        System.cpu.reset()

        // TODO: test, see http://visual6502.org/wiki/index.php?title=6502TestPrograms
        //memory.loadIntoRam("./roms/6502_functional_test.bin")
        //registers.PC = 0x0400
        System.cpu.runMachine()
    }
}

@ExperimentalUnsignedTypes
fun main (args : Array<String>) {
    BootstrapC64()
}