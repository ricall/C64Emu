package c64.emulation

import c64.emulation.cpu.CPU
import c64.emulation.cpu.Registers
import c64.emulation.memory.Memory
import c64.emulation.vic.VIC

/**
 * Class which holds all parts of the emulated machine (CPU, VIC, ...) and initialize them in
 * the right order.
 *
 * @author Daniel Schulte 2017-2019
 */
@ExperimentalUnsignedTypes
object System {

    internal var registers: Registers = Registers()
    internal var memory: Memory = Memory()
    internal var vic: VIC = VIC()
    // initialize CPU at last
    internal var cpu: CPU = CPU()

    init {
        cpu.initialize()
    }
}