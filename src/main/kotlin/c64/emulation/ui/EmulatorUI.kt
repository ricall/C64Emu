package c64.emulation.ui

import c64.emulation.BootstrapC64
import c64.emulation.System
import c64.emulation.System.keyboard
import c64.emulation.System.vic
import c64.emulation.vic.VIC
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KLogging
import java.awt.Dimension
import java.util.Timer
import javax.swing.JFrame
import kotlin.concurrent.schedule
import kotlin.concurrent.timer

/**
 * Simple UI to show the display of the emulator.
 *
 * @author Daniel Schulte 2017-2021
 */
@ExperimentalUnsignedTypes
class EmulatorUI {

    companion object : KLogging()

    init {
        val frame = JFrame()
        frame.layout = null
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.size = Dimension(VIC.PAL_RASTERCOLUMNS * 2, VIC.PAL_RASTERLINES * 2)
        frame.isVisible = true

        frame.addKeyListener(keyboard)

        GlobalScope.launch {
            BootstrapC64()
        }

        // 40ms ~= 25 frames/s
        timer("display_refresh", false, 100, 40) {
            frame.graphics.drawImage(vic.bitmapData, 0, 0,
                vic.bitmapData.width * 2, vic.bitmapData.height * 2, frame)
        }

        // load basic test program
        Timer().schedule(3000) {
            System.memory.loadPrg("./src/test/c64/prg/clock compute.prg")
        }
    }
}

@ExperimentalUnsignedTypes
fun main() {
    EmulatorUI()
}
