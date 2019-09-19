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
class Keyboard : KeyListener {

    companion object {
        var SHIFT_CODE = 0xFF
        var keyboardTranslationMatrix = arrayOf(
            //         DEL   RET   C-RI  F7    F1    F3    F5    C-DO
            intArrayOf(0x00, 0x0A, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
            //         3     W     A     4     Z     S     E     LSHI
            intArrayOf(0x33, 0x57, 0x41, 0x34, 0x5A, 0x53, 0x45, 0xFF),
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
            intArrayOf(0x31, 0x3C, 0x00, 0x32, 0x20, 0x00, 0x51, 0x00)
        )
        var keyboardTranslation = hashMapOf(KeyEvent.VK_PLUS to 0x2B, KeyEvent.VK_0 to 0x3D)
        // todo var keyboardShiftTranslation = hashMapOf(KeyEvent.VK_PLUS to 0x2B, KeyEvent.VK_0 to 0x3D)
    }

    private var shiftState: Int = -1
    private var lastKeyCode: Int = -1

    fun getDataPortB(dataPortA: UByte): UByte {
        var result = 0
        if (lastKeyCode > -1) {
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
                val row = keyboardTranslationMatrix[col]
                // find key code
                var keyIndex = row.indexOf(lastKeyCode)
                if (keyIndex > -1) {
                    result = result or (1 shl keyIndex)
                }
                // find shift
                keyIndex = row.indexOf(shiftState)
                if (keyIndex > -1) {
                    result = result or (1 shl keyIndex)
                }
            }
        }
        return (result xor 0xFF).toUByte()
    }

    override fun keyPressed(e: KeyEvent?) {
        if (e != null) {
            shiftState = -1
            lastKeyCode = e.keyCode
            val shiftDown = e.modifiersEx and InputEvent.SHIFT_DOWN_MASK == InputEvent.SHIFT_DOWN_MASK
            if (shiftDown) {
                when (lastKeyCode) {
                    in KeyEvent.VK_A..KeyEvent.VK_Z,
                    in KeyEvent.VK_1..KeyEvent.VK_2,
                    in KeyEvent.VK_4..KeyEvent.VK_6,
                    in KeyEvent.VK_8..KeyEvent.VK_9 -> {
                        // keys A-Z, 1-2, 4-6, 8-9
                        // ==> normal shift handling
                        shiftState = SHIFT_CODE
                    }
                    KeyEvent.VK_0 -> {
                        lastKeyCode = keyboardTranslation[lastKeyCode]
                    }
                    KeyEvent.VK_7 -> {
                        // shift+7 => /
                        lastKeyCode = 0x2F
                    }
                    KeyEvent.VK_PERIOD -> {
                        // shift+. => :
                        lastKeyCode = 0x3A
                    }
                    KeyEvent.VK_COMMA -> {
                        // shift+, => ;
                        lastKeyCode = 0x3B
                    }
                }
            }
            else {
                // todo: ? * # ' (^ should be left arrow) < > ESC (BACK shoudl be DEL) CURSOR-KEY
                when (lastKeyCode) {
                    KeyEvent.VK_PLUS -> {
                        // translate +
                        lastKeyCode = keyboardTranslation[lastKeyCode]
                    }
                }
            }
        } else {
            lastKeyCode = -1
        }
    }

    override fun keyReleased(e: KeyEvent?) {
        lastKeyCode = -1
    }

    override fun keyTyped(e: KeyEvent?) {
        // nothing
    }
}