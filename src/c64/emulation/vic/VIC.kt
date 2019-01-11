package c64.emulation.vic

import c64.emulation.System.memory
import c64.emulation.System.registers
import mu.KotlinLogging
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

private val logger = KotlinLogging.logger {}

/**
 * Emulation of the C64 video chip VIC-II - MOS 6567/6569.
 * Emulation only for PAL!
 *
 * @author Daniel Schulte 2017-2019
 */
@ExperimentalUnsignedTypes
class VIC {

    // TODO: handle interrupt status in $D019
    // TODO: handle background color at $D021
    // TODO: handle border color at $D020
    // TODO: registers are from $D000-D3FF, repeating every 64bytes 16x ($D000, $D040, $D080,...)
    // TODO: handle screen-memory AND charset (in current video-bank) in $D018
    // TODO: color ram always at $D800
    // TODO: set used video-bank (bit 0+1) in $DD00 (CIA2) (+$DD02 Port A data direction register)

    companion object {
        const val PAL_RASTERLINES: Int = 312
        const val PAL_RASTERCOLUMNS: Int = 367
        const val PAL_CYCLES_PER_RASTERLINE: Int = 63

        const val NTSC_RASTERLINES: Int = 263
        const val NTSC_CYCLES_PER_RASTERLINE: Int = 65
    }

    private val bitmapData: BufferedImage
    private var lastRasterLine: Int = 0

    init {
        bitmapData = BufferedImage(PAL_RASTERCOLUMNS, PAL_RASTERLINES, BufferedImage.TYPE_3BYTE_BGR)
    }

    fun saveScreenshot(filename: String) {
        val file: File = File(filename)
        ImageIO.write(bitmapData, "png", file)
    }

    internal fun refresh() {
        // PAL systems (50Hz) uses ~312 rasterlines (means ~63 cycles per line), visible lines: 284 (16-299)
        // NTSC systems (60Hz) uses ~263 rasterlines (means ~65 cycles per line), visible lines: 235 (...)
        // calc current rasterline
        val frame: Long = registers.cycles / (PAL_RASTERLINES * PAL_CYCLES_PER_RASTERLINE)
        val line: Int = ((registers.cycles  - frame * PAL_RASTERLINES * PAL_CYCLES_PER_RASTERLINE) / PAL_CYCLES_PER_RASTERLINE).toInt()
        if (lastRasterLine != line) {
            // new rasterline starts now
            // store new line positon in $D012 + $D011
            memory.store(0xD012, line.toUByte())
            // TODO: store bit 8 of current line in bit 7 of $D011
            // raster last finished line to bitmap
            rasterLine(lastRasterLine)
            lastRasterLine = line
        }
    }

    private fun rasterLine(rasterline: Int) {
        // display window from rasterline 51 - 250 (=200 lines)
        // display window from rastercolumn 24 - 343 (=320px)
        val bitmapMode = memory.fetch(0xD011) and 0b0010_0000u
        val y:Int = rasterline - 51
        if (bitmapMode.toInt() == 0) {
            // text-mode
            for (rastercolumn in 0 until PAL_RASTERCOLUMNS) {
                var color: Int
                if (rasterline < 51 || rasterline > 250 || rastercolumn < 24 || rastercolumn > 343) {
                    // outer border color
                    color = 0x867ADE
                }
                else {
                    // display window
                    val x = rastercolumn - 24
                    val addr = getScreenMemoryAddress()
                    val textRow = y / 8
                    val textCol = x / 8
                    val screenAddr = addr + textRow * 40 + textCol
                    val char: UByte = fetchFromVideoBank(screenAddr)

                    val charY = y.rem(8)
                    val charX = x.rem(8)
                    val rawCharData = fetchFromCharMemory(char.toInt() * 8 + charY)
                    val pixelMask: UByte = (0b1000_0000u shr charX).toUByte()
                    val pixel = rawCharData and pixelMask
                    color = if (pixel == pixelMask)
                        0x867ADE  // outer border color
                    else
                        0x483AAA // default background color
                }
                bitmapData.setRGB(rastercolumn, rasterline, color)
            }
        }
        else {
            // TODO: handle bitmap-mode
        }
    }

    private fun fetchFromVideoBank(address: Int): UByte {
        // select video bank + translate address
        // vic bank
        // 0: bank 3 $C000-$FFFF
        // 1: bank 2 $8000-$BFFF
        // 2: bank 1 $4000-$7FFF
        // 3: bank 0 $0000-$3FFF
        val vicBank = memory.fetch(0xDD00) and 0b0000_0011u
        val translatedAddr: Int = address + (0xC000 - vicBank.toInt() * 0x4000)
        return memory.fetch(translatedAddr)
    }

    private fun fetchFromCharMemory(offset: Int): UByte {
        val vicBank = (memory.fetch(0xDD00) and 0b0000_0011u).toInt()
        val b = (memory.fetch(0xD018) and 0b0000_1110u).toInt() shr 1
        val address = offset + b * 0x800
        return if ((b == 2 || b == 3) && (vicBank == 3 || vicBank == 1)) {
            // get value from char ROM if
            // * VIC bank 0 or 2 is selected  AND
            // * char mem pointer is 2 or 3
            memory.fetchFromCharROM(address)
        } else {
            fetchFromVideoBank(address)
        }
    }

    private fun getScreenMemoryAddress(): Int {
        val b = (memory.fetch(0xD018) and 0b1111_0000u).toInt() shr 4
        return b * 0x400
    }
}