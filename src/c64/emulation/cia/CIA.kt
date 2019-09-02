package c64.emulation.cia

import c64.emulation.System.cpu
import c64.util.toBinary
import c64.util.toHex
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Emulation of the C64 Complex Interface Adapter (CIA 6526).
 *
 * @author Daniel Schulte 2017-2019
 */
@ExperimentalUnsignedTypes
class CIA {

    companion object {
        val CIA_ADDRESS_SPACE = 0xDC00..0xDCFF
        const val DATA_PORT_A: Int = 0x00
        const val DATA_PORT_B: Int = 0x01
        const val DATA_DIRECTION_A: Int = 0x02
        const val DATA_DIRECTION_B: Int = 0x03
        const val TIMER_A_LOW: Int = 0x04
        const val TIMER_A_HIGH: Int = 0x05
        const val TIMER_B_LOW: Int = 0x06
        const val TIMER_B_HIGH: Int = 0x07

        const val TODTEN: Int = 0x08  // Time of Day Clock Tenths of Seconds
        const val TODSEC: Int = 0x09  // Time of Day Clock Seconds
        const val TODMIN: Int = 0x0A  // Time of Day Clock Minutes
        const val TODHRS: Int = 0x0B  // Time of Day Clock Hours

        const val CIASDR: Int = 0x0C  // Serial Data Port
        const val CIAICR: Int = 0x0D  // Interrupt Control Register
        const val CIACRA: Int = 0x0E  //  Control Register A
        const val CIACRB: Int = 0x0F  //  Control Register B
    }

    private var timerAIRQEnabled = false
    private var timerAEnabled = false
    private var timerARunMode = 0
    private var timerALatch: Int = 0x0000
    private var timerA: Int = 0x0000
    private var timerBIRQEnabled = false
    private var timerBLatch: Int = 0x0000
    private var timerB: Int = 0x0000


    /**
     * Signals the next cycle
     */
    fun cycle() {
        if (timerAEnabled) {
            //logger.info {"counting cycle down to $timerA"}
            timerA--
            if (timerA <= 0) {
                // timer has reached 0
                // reset timer to latch
                timerA = timerALatch
                if (timerARunMode == 8) {
                    // timer is in one-shot mode ==> stop timer
                    timerAEnabled = false
                }
                if (timerAIRQEnabled) {
                    // signal Timer A interrupt
                    cpu.signalTimerAIRQ()
                }
            }
        }
    }

    /**
     * Fetches a single byte from the given CIA address.
     */
    fun fetch(address: Int): UByte {
        when (address and 0x000F) {
            /*CIAICR -> {
                // todo - implementation
                return 0x00.toUByte()
            }
            CIACRA -> {
                // todo - implementation
                return 0x00.toUByte()
            }*/
            else -> {
                // todo - implementation
                logger.info { "missing IMPL for CIA1:read ${address.toHex()}" }
                return 0x00.toUByte()
            }
        }
    }

    /**
     * Stores a single byte at the given CIA address.
     */
    fun store(address: Int, byte: UByte) {
        // use only bit 0-4, mask out all higher bits
        when (address and 0x000F) {
            DATA_PORT_A -> {
                //logger.info { "missing IMPL for DATA_PORT_A:write ${byte.toHex()}" }
            }
            DATA_PORT_B -> {
                //logger.info { "missing IMPL for DATA_PORT_B:write ${byte.toHex()}" }
            }
            DATA_DIRECTION_A -> {
                //logger.info { "missing IMPL for DATA_DIRECTION_A:write ${byte.toHex()}" }
            }
            DATA_DIRECTION_B -> {
                //logger.info { "missing IMPL for DATA_DIRECTION_B:write ${byte.toHex()}" }
            }
            TIMER_A_LOW -> {
                logger.info { "write to TIMER_A_LOW: ${byte.toHex()}" }
                timerALatch = (timerALatch and 0xFF00) + byte.toInt()
            }
            TIMER_A_HIGH -> {
                logger.info { "write to TIMER_A_HIGH: ${byte.toHex()}" }
                timerALatch = (timerALatch and 0x00FF) + (byte.toInt() shl 8)
            }
            TIMER_B_LOW -> {
                logger.info { "write to TIMER_B_LOW register: ${byte.toHex()}" }
                timerBLatch = (timerBLatch and 0xFF00) + byte.toInt()
            }
            TIMER_B_HIGH -> {
                logger.info { "write to TIMER_B_HIGH register: ${byte.toHex()}" }
                timerBLatch = (timerBLatch and 0x00FF) + (byte.toInt() shl 8)
            }
            TODTEN -> {
                //logger.info { "missing IMPL for TODTEN:write ${byte.toHex()}"}
            }
            TODSEC -> {
                //logger.info { "missing IMPL for TODSEC:write ${byte.toHex()}" }
            }
            TODMIN -> {
                //logger.info { "missing IMPL for TODMIN:write ${byte.toHex()}" }
            }
            TODHRS -> {
                //logger.info { "missing IMPL for TODHRS:write ${byte.toHex()}" }
            }
            CIASDR -> {
                //logger.info { "missing IMPL for CIASDR:write ${byte.toHex()}" }
            }
            CIAICR -> {
                writeCIAICR(byte)
            }
            CIACRA -> {
                writeCIACRA(byte)
            }
            CIACRB -> {
                writeCIACRB(byte)
            }
        }
    }

    private fun writeCIAICR(byte: UByte) {
        logger.info {"write to CIAICR register: ${byte.toBinary()}"}
        // BIT 7: set or clear bits (0=clear bits, 1=set bits)
        val setMode = byte.toInt() and 0b1000_0000 == 0b1000_0000
        // BIT 0: enable / disable Timer A interrupt
        if (byte.toInt() and 0b0000_0001 == 0b0000_0001) {
            timerAIRQEnabled = setMode
            logger.info {"timerAIRQEnabled: $timerAIRQEnabled"}
        }
        // check for missing implementation
        if (byte.toInt() and 0b0111_1110 > 0) {
            // todo: implementation
            logger.warn { "not handled BITS for CIAICR register: ${byte.toBinary()}" }
        }
    }

    private fun writeCIACRA(byte: UByte) {
        // todo: implementation
        logger.info { "write to CIACRA register: ${byte.toBinary()}" }
        // BIT 0: start/stop Timer A
        timerAEnabled = byte.toInt() and 0b0000_0001 == 0b0000_0001
        logger.info {"timerAEnabled: $timerAEnabled"}
        // BIT 3: Timer A run mode: 1=one-shot, 0=continous
        timerARunMode = byte.toInt() and 0b0000_1000
        logger.info { "timerARunMode: $timerARunMode" }
        // BIT 4: load latch Timer A
        if (byte.toInt() and 0b0001_0000 == 0b0001_0000) {
            timerA = timerALatch
        }
        // check for missing implementation
        if (byte.toInt() and 0b1110_0110 > 0) {
            // todo: implementation
            logger.warn { "not handled BITS for writeCIACRA register: ${byte.toBinary()}" }
        }
    }

    private fun writeCIACRB(byte: UByte) {
        // todo: implementation
        logger.info {"write to CIACRB register: ${byte.toBinary()}"}
    }
}