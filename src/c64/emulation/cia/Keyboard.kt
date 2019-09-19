package c64.emulation.cia

import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.KeyListener

/**
 * Class which handles Keyboard input and translates the incoming keyCodes for the CIA.
 *
 * @author Daniel Schulte 2017-2019
 */
@ExperimentalUnsignedTypes
class Keyboard: KeyListener {

    companion object {
        var keyboardMatrix = arrayOf(
            //         DEL   RET   C-RI  F7    F1    F3    F5    C-DO
            intArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
            //         3     W     A     4     Z     S     E     LSHI
            intArrayOf(0x33, 0x57, 0x41, 0x34, 0x5A, 0x53, 0x45, 0x00),
            //         5     R     D     6     C     F     T     X
            intArrayOf(0x35, 0x52, 0x44, 0x36, 0x43, 0x46, 0x54, 0x58),
            //         7     Y     G     8     B     H     U     V
            intArrayOf(0x37, 0x59, 0x47, 0x38, 0x42, 0x48, 0x55, 0x56),
            //         9     I     J     0     M     K     O     N
            intArrayOf(0x39, 0x49, 0x4A, 0x30, 0x4D, 0x4B, 0x4F, 0x4E),
            //         +     P     L     -     .     :     @     ,
            intArrayOf(0x2B, 0x50, 0x4C, 0x2D, 0x2E, 0x3A, 0x40, 0x2C),
            //         £     *     ;     HOME  RSHI  =     ^     /
            intArrayOf(0x9C, 0x2A, 0x3B, 0x00, 0x00, 0x3D, 0x5E, 0x2F),
            //         1     <     CTRL  2     SPACE C=    Q     RUN/STOP
            intArrayOf(0x31, 0x3C, 0x00, 0x32, 0x20, 0x00, 0x51, 0x00))
    }

    private var shiftState: Int = -1
    private var lastCharCode: Int = -1

    fun getDataPortB(dataPortA: UByte): UByte {
        var result = 0
        if (lastCharCode > -1) {
            var column = 0
            val columns = ArrayList<Int>()
            var dataPortAInt = dataPortA.toInt() xor 0xFF
            // collect all columns to read
            while (dataPortAInt > 0) {
                if (dataPortAInt and 0x01 == 0x01) {
                    columns.add(column)
                }
                column++
                dataPortAInt = dataPortAInt shr 1
            }
            // find all pressed keys
            for (col in columns) {
                val row = keyboardMatrix[col]
                val keyIndex = row.indexOf(lastCharCode)
                if (keyIndex > -1) {
                    result = result or (1 shl keyIndex)
                }
            }
        }
        return (result xor 0xFF).toUByte()
    }

    override fun keyPressed(e: KeyEvent?) {
        if (e != null) {
            if (e.modifiersEx and InputEvent.SHIFT_DOWN_MASK == InputEvent.SHIFT_DOWN_MASK) {
                shiftState = 1
            }
            else {
                shiftState = 0
            }
            lastCharCode = e.keyChar.toInt()
        }
        else {
            lastCharCode = -1
        }
    }

    override fun keyReleased(e: KeyEvent?) {
        lastCharCode = -1
    }

    override fun keyTyped(e: KeyEvent?) {
        // nothing
    }
}