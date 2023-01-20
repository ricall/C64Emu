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
    // TODO: set used video-bank (bit 0+1) in $DD00 (CIA2) (+$DD02 Port A data direction register)

    companion object {
        const val PAL_RASTERLINES: Int = 312
        const val PAL_RASTERCOLUMNS: Int = 367
        const val PAL_CYCLES_PER_RASTERLINE: Int = 63
        const val PAL_CYCLES_PER_FRAME: Int = PAL_RASTERLINES * PAL_CYCLES_PER_RASTERLINE

        const val NTSC_RASTERLINES: Int = 263
        const val NTSC_CYCLES_PER_RASTERLINE: Int = 65

        ///////////////////////////////////////////
        // registers are from $D000-D3FF, repeating every 64bytes 16x ($D000, $D040, $D080,...)
        ///////////////////////////////////////////
        // Vertical Fine Scrolling and Control Register
        const val VIC_SCROLY = 0xD011
        // Read Current Raster Scan Line/Write Line to Compare for Raster IRQ
        const val VIC_RASTER = 0xD012
        // Horizontal Fine Scrolling and Control Register
        const val VIC_SCROLX = 0xD016
        // Chip Memory Control Register
        const val VIC_VMCSB = 0xD018
        // Border Color Register
        const val VIC_EXTCOL = 0xD020
        // Background Color 0
        const val VIC_BGCOL1 = 0xD021

        // color ram $D800-DBFF
        const val COLOR_RAM = 0xD800

        /*val COLOR_TABLE = arrayOf(
            0x000000, 0xFFFFFF, 0xAF2A29, 0x62D8CC, 0xB03FB6, 0x4AC64A, 0x3739C4, 0xE4ED4E,
            0xB6591C, 0x683808, 0xEA746C, 0x4D4D4D, 0x848484, 0xA6FA9E, 0x707CE6, 0xB6B6B5
        )*/
        val COLOR_TABLE = arrayOf(
            //black,  white,    red,      cyan,     purple,   green,    blue,     yellow
            0x000000, 0xFFFFFF, 0x924A40, 0x84C5CC, 0x9351B6, 0x72B14B, 0x483AAA, 0xD5DF7C,
            //orange, brown,    pink,    dark grey, gray,    lt green, lt blue,   lt gray
            0x99692D, 0x675200, 0xC18178, 0x606060, 0x8A8A8A, 0xB3EC91, 0x867ADE, 0xB3B3B3
        )
    }

    val bitmapData: BufferedImage
    private var lastRasterLine: Int = 0

    init {
        logger.info { "init VIC" }
        bitmapData = BufferedImage(PAL_RASTERCOLUMNS, PAL_RASTERLINES, BufferedImage.TYPE_3BYTE_BGR)
    }

    fun saveScreenshot(filename: String) {
        ImageIO.write(bitmapData, "png", File(filename))
    }

    internal fun refresh() {
        // PAL systems (50Hz) uses ~312 rasterlines (means ~63 cycles per line), visible lines: 284 (16-299)
        // NTSC systems (60Hz) uses ~263 rasterlines (means ~65 cycles per line), visible lines: 235 (...)
        // calc current rasterline
        val line: Int = (registers.cycles.rem(PAL_CYCLES_PER_FRAME) / PAL_CYCLES_PER_RASTERLINE).toInt()
        if (lastRasterLine != line) {
            // new rasterline starts now
            // store new line positon in $D012 + $D011
            memory.store(VIC_RASTER, line.toUByte())
            // TODO: store bit 8 of current line in bit 7 of $D011
            // raster last finished line to bitmap
            rasterLine(lastRasterLine)
            lastRasterLine = line
        }
    }

    private fun rasterLine(rasterline: Int) {
        // display window from rasterline 51 - 250 (=200 lines)
        // display window from rastercolumn 24 - 343 (=320px)
        val bitmapMode = memory.fetch(VIC_SCROLY) and 0b0010_0000u
        val y: Int = rasterline - 51
        if (bitmapMode.toInt() == 0) {
            // text-mode
            rasterTextMode(rasterline, y)
        }
        else {
            // bitmap mode
            rasterBitmapMode(rasterline, y)
        }
    }

    private fun rasterTextMode(rasterline: Int, y: Int) {
        // todo: multicolor text mode: set by bit 4 of $d016
        // todo: Extended Background Color Mode
        // todo: SCROLX bit 3 - 38/40 column mode
        val borderColor = COLOR_TABLE[memory.fetch(VIC_EXTCOL).toInt() and 0b0000_1111]
        val backgroundColor = COLOR_TABLE[memory.fetch(VIC_BGCOL1).toInt() and 0b0000_1111]
        val screenMemoryAddress = getScreenMemoryAddress()
        val videoBankAddress = getVideoBankAddress()
        val fetchFromCharMemoryFunction = getFetchFromCharMemoryFunction()
        val textRow = y / 8
        val textRowAddr = textRow * 40
        val charY = y.rem(8)

        val colorRamRowAddress = COLOR_RAM + textRowAddr
        val screenMemoryRowAddress = videoBankAddress + screenMemoryAddress + textRowAddr

        for (rastercolumn in 0 until PAL_RASTERCOLUMNS) {
            var color: Int
            if (rasterline < 51 || rasterline > 250 || rastercolumn < 24 || rastercolumn > 343) {
                // outer border color
                color = borderColor
            } else {
                // display window
                val x = rastercolumn - 24
                val textCol = x / 8
                val char = memory.fetch(screenMemoryRowAddress + textCol)
                val charColor = COLOR_TABLE[memory.fetch(colorRamRowAddress + textCol).toInt() and 0b0000_1111]

                val charX = x and 0b0000_0111
                // todo: small optimization possible - fetchFromVideoBank should be replaced
                val rawCharData = fetchFromCharMemoryFunction(char.toInt() * 8 + charY)
                val pixelMask: UByte = (0b1000_0000u shr charX).toUByte()
                color = if (rawCharData and pixelMask == pixelMask)
                    charColor
                else
                    backgroundColor
            }
            bitmapData.setRGB(rastercolumn, rasterline, color)
        }
    }

    private fun rasterBitmapMode(rasterline: Int, y: Int) {
        val isMulticolorMode = memory.fetch(VIC_SCROLX).toInt() and 0b0001_0000 == 0b0001_0000
        val videoBankAddress = getVideoBankAddress()
        val borderColor = COLOR_TABLE[memory.fetch(VIC_EXTCOL).toInt() and 0b0000_1111]
        val textRow = y / 8
        val textRowAddr = textRow * 40
        val charY = y.rem(8)
        val bitmapColorRowAddress = videoBankAddress + getBitmapColorAddress() + textRowAddr
        val bitmapRowAddress = videoBankAddress + getBitmapAddress() + textRowAddr * 8 + charY
        val colorRamRowAddress = COLOR_RAM + textRowAddr
        val backgroundColor = COLOR_TABLE[memory.fetch(VIC_BGCOL1).toInt() and 0b0000_1111]

        if (!isMulticolorMode) {
            // hires bitmap mode
            for (rastercolumn in 0 until PAL_RASTERCOLUMNS) {
                var color: Int
                if (rasterline < 51 || rasterline > 250 || rastercolumn < 24 || rastercolumn > 343) {
                    // outer border color
                    color = borderColor
                } else {
                    val x = rastercolumn - 24
                    val textCol = x / 8
                    val pxBit = x and 0b0000_0111
                    val colors = memory.fetch(bitmapColorRowAddress + textCol).toInt()
                    // 8x8 colorblock - hi-nibble: pixel-color, lo-nibble: background-color
                    val bgColor = COLOR_TABLE[colors and 0b0000_1111]
                    val pxColor = COLOR_TABLE[colors and 0b1111_0000 shr 4]

                    // get byte from bitmap
                    val bitmapByte = memory.fetch(bitmapRowAddress + textCol * 8).toInt()
                    // read the correct bit
                    val bitTest = (0b1000_0000u shr pxBit).toInt()
                    color = if (bitmapByte and bitTest == bitTest) {
                        pxColor
                    } else {
                        bgColor
                    }
                }
                bitmapData.setRGB(rastercolumn, rasterline, color)
            }
        } else {
            // todo - later merge code with code for hires...
            // multicolor bitmap mode
            for (rastercolumn in 0 until PAL_RASTERCOLUMNS) {
                var color: Int
                if (rasterline < 51 || rasterline > 250 || rastercolumn < 24 || rastercolumn > 343) {
                    // outer border color
                    color = borderColor
                } else {
                    val x = rastercolumn - 24
                    val textCol = x / 8
                    val pxBit = x and 0b0000_0110
                    val colors = memory.fetch(bitmapColorRowAddress + textCol).toInt()
                    // 8x8 colorblock - hi-nibble: hi-color, lo-nibble: lo-color
                    val lo_color = COLOR_TABLE[colors and 0b0000_1111]
                    val hi_color = COLOR_TABLE[colors and 0b1111_0000 shr 4]
                    val color_ram = COLOR_TABLE[memory.fetch(colorRamRowAddress + textCol).toInt() and 0b0000_1111]

                    // get byte from bitmap
                    val bitmapByte = memory.fetch(bitmapRowAddress + textCol * 8).toInt()
                    // read the correct bit
                    val bitTest = (0b1100_0000u shr pxBit).toInt()
                    color = when ((bitmapByte and bitTest) shr (6-pxBit)) {
                        0b00 -> {
                            backgroundColor
                        }
                        0b01 -> {
                            hi_color
                        }
                        0b10 -> {
                            lo_color
                        }
                        else -> {
                            color_ram
                        }
                    }
                }
                bitmapData.setRGB(rastercolumn, rasterline, color)
            }
        }
    }

    private fun getVideoBank(): Int {
        return (memory.fetch(0xDD00) and 0b0000_0011u).toInt()
    }

    private fun getVideoBankAddress(): Int {
        // select video bank + translate address
        // vic bank
        // 0: bank 3 $C000-$FFFF
        // 1: bank 2 $8000-$BFFF
        // 2: bank 1 $4000-$7FFF
        // 3: bank 0 $0000-$3FFF
        return 0xC000 - getVideoBank() * 0x4000
    }

    private fun fetchFromVideoBank(address: Int): UByte {
        return memory.fetch(address + getVideoBankAddress())
    }

    private fun fetchFromCharMemory(offset: Int): UByte {
        val vicBank = getVideoBank()
        val characterDotDataIndex = (memory.fetch(VIC_VMCSB) and 0b0000_1110u).toInt() shr 1
        val characterDotDataAddress = offset + characterDotDataIndex * 0x800
        return if ((characterDotDataIndex == 2 || characterDotDataIndex == 3) && (vicBank == 3 || vicBank == 1)) {
            // get value from char ROM if
            // * VIC bank 0 or 2 is selected  AND
            // * char mem pointer is 2 or 3
            memory.fetchFromCharROM(characterDotDataAddress)
        } else {
            fetchFromVideoBank(characterDotDataAddress)
        }
    }

    private fun getFetchFromCharMemoryFunction(): (offset: Int) -> UByte {
        val vicBank = getVideoBank()
        val characterDotDataIndex = (memory.fetch(VIC_VMCSB) and 0b0000_1110u).toInt() shr 1
        return if ((characterDotDataIndex == 2 || characterDotDataIndex == 3) && (vicBank == 3 || vicBank == 1)) {
            // get value from char ROM if
            // * VIC bank 0 or 2 is selected  AND
            // * char mem pointer is 2 or 3
            memory::fetchFromCharROM
        } else {
            ::fetchFromVideoBank
        }
    }

    private fun getScreenMemoryAddress(): Int {
        return 0x0400 * ((memory.fetch(VIC_VMCSB) and 0b1111_0000u).toInt() shr 4)
    }

    private fun getBitmapAddress(): Int {
        // bit 3 of VIC_VMCSB controls start of the bitmap data
        return if (memory.fetch(VIC_VMCSB).toInt() and 0b0000_01000 == 0b0000_01000) {
            0x2000
        } else {
            0x0000
        }
    }

    private fun getBitmapColorAddress(): Int {
        return getScreenMemoryAddress()
    }
}