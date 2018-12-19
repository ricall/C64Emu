package c64.emulation.memory

import c64.emulation.Registers
import c64.util.toHex
import c64.util.toUnprefixedHex
import mu.KotlinLogging
import java.io.File
import java.nio.file.Files

private val logger = KotlinLogging.logger {}

/**
 * Class which encapsulates all operations on memory.
 * todo: bank switching:  https://www.c64-wiki.com/wiki/Bank_Switching
 *
 * @author Daniel Schulte 2017-2018
 */
@ExperimentalUnsignedTypes
class Memory {

    companion object {
        const val MEM_SIZE: Int = 65536
        const val KERNAL_FILE: String = "./roms/kernal"
        const val KERNAL_OFFSET: Int = 0xE000
        const val KERNAL_SIZE: Int = 8192
        const val BASIC_FILE: String = "./roms/basic"
        const val BASIC_OFFSET: Int = 0xA000
        const val BASIC_SIZE: Int = 8192
        const val STACK_OFFSET: Int = 0x0100

        /**
         * Creates a Word from the given Hi- and Lo-Byte and returns it as Int.
         */
        fun wordFromLoHi(lo: UByte, hi: UByte): Int {
            return lo.toInt() + (hi.toInt() shl 8)
        }

        /**
         * Returns the Lo-Byte from the given Word.
         */
        fun loByteFromWord(word: Int): UByte {
            return (word and 0xFF).toUByte()
        }

        /**
         * Returns the Hi-Byte from the given Word.
         */
        fun hiByteFromWord(word: Int): UByte {
            return (word shr 8).toUByte()
        }

        /**
         * Checks two addresses (one absolute address and the second as indexed address) for a "page boundary cross",
         * means it checks whether both addresses are * on the same address page. An address page is $FF byte in size.
         *
         * If the addresses are NOT on the same page, "1" as cycle penalty is returned, otherwise "0"
         * for no cycle penalty is returned.
         */
        fun checkForPageBoundaryCross(address: Int, index: UByte): Int {
            return checkForPageBoundaryCross(address, address + index.toInt())
        }

        /**
         * Checks two addresses for a "page boundary cross", means it checks whether both addresses are
         * on the same address page. An address page is $FF byte in size.
         *
         * If the addresses are NOT on the same page, "1" as cycle penalty is returned, otherwise "0"
         * for no cycle penalty is returned.
         */
        fun checkForPageBoundaryCross(address1: Int, address2: Int): Int {
            val page: Int = address1 and 0xFF00
            val indexedPage: Int = address2 and 0xFF00
            return if (page != indexedPage) 1 else 0
        }
    }

    private val mem: UByteArray

    // registers
    internal lateinit var registers: Registers

    init {
        logger.info { "init Memory with size of <$MEM_SIZE> byte." }
        mem = UByteArray(MEM_SIZE)
        logger.info { "loading kernal" }
        load(
            KERNAL_FILE,
            KERNAL_OFFSET,
            KERNAL_SIZE
        )
        logger.info { "loading basic" }
        load(
            BASIC_FILE,
            BASIC_OFFSET,
            BASIC_SIZE
        )
    }

    @Suppress("unused")
    fun loadRom(filename: String, address: Int = 0x0000) {
        load(filename, address, -1)
    }

    private fun load(filename: String, address: Int, expectedSize: Int) {
        logger.debug { "loading <$filename> @${address.toHex()}" }
        val file = File(filename)
        if (!file.exists() || (expectedSize > -1 && file.length() != expectedSize.toLong())) {
            throw RuntimeException("invalid file $filename")
        }

        val buffer: UByteArray = Files.readAllBytes(file.toPath()).toUByteArray()
        buffer.copyInto(mem, address)
    }

    /**
     * Fetches a single byte from the given address.
     */
    fun fetch(address: Int): UByte {
        // todo: later optimization - maybe use @inline
        // todo: check address for access ram or rom
        // use only bit 0-15, mask out all higher bits
        return mem[address and 0xFFFF]
    }

    /**
     * Fetches a single byte from the zero page address stored at PC.
     */
    fun fetchZeroPageWithPC(): UByte {
        return fetch(fetchZeroPageAddressWithPC())
    }

    /**
     * Fetches a single byte from the zero page address stored at PC indexed with X,
     * with respect to the zero page boundaries.
     */
    fun fetchZeroPageXWithPC(): UByte {
       return fetch(fetchZeroPageXAddressWithPC())
    }

    /**
     * Fetches a single byte from the zero page address stored at PC indexed with Y,
     * with respect to the zero page boundaries.
     */
    fun fetchZeroPageYWithPC(): UByte {
       return fetch(fetchZeroPageYAddressWithPC())
    }

    /**
     * Addressing mode: absolute
     *
     * Fetches a single byte from the address stored at PC.
     */
    fun fetchAbsoluteWithPC(): UByte {
        return fetch(fetchWordWithPC())
    }

    /**
     * Addressing mode: absolute, x
     *
     * Fetches a single byte from the given address + X.
     * In case of a page boundary cross the cycle counter will be increased by one!
     */
    fun fetchAbsoluteX(address: Int): UByte {
        return fetchAbsoluteIndexed(address, registers.X)
    }

    /**
     * Addressing mode: absolute, x
     *
     * Fetches a single byte from the address stored at PC + X.
     * In case of a page boundary cross the cycle counter will be increased by one!
     */
    fun fetchAbsoluteXWithPC(): UByte {
        return fetchAbsoluteX(fetchWordWithPC())
    }

    /**
     * Addressing mode: absolute, y
     *
     * Fetches a single byte from the given address + Y.
     * In case of a page boundary cross the cycle counter will be increased by one!
     */
    fun fetchAbsoluteY(address: Int): UByte {
        return fetchAbsoluteIndexed(address, registers.Y)
    }

    /**
     * Addressing mode: absolute, y
     *
     * Fetches a single byte from the address stored at PC + Y.
     * In case of a page boundary cross the cycle counter will be increased by one!
     */
    fun fetchAbsoluteYWithPC(): UByte {
        return fetchAbsoluteY(fetchWordWithPC())
    }

    private fun fetchAbsoluteIndexed(address: Int, index: UByte): UByte {
        // check for page boundary cross
        registers.cycles += checkForPageBoundaryCross(address, index)
        return fetch(address + index.toInt())
    }

    /**
     * Addressing mode: (indirect,X)
     *
     * Fetches a single byte from the address stored at (zero-page + X)
     */
    fun fetchIndexedIndirectXWithPC(): UByte {
        val addr = fetchZeroPageXAddressWithPC()
        return fetch(wordFromLoHi(fetch(addr), fetch((addr + 1) and 0xFF)))
    }

    /**
     * Addressing mode: (indirect,X)
     *
     * Fetches an address stored at (zero-page + X)
     */
    fun fetchIndexedIndirectXAddressWithPC(): Int {
        val addr = fetchZeroPageXAddressWithPC()
        return wordFromLoHi(fetch(addr), fetch((addr + 1) and 0xFF))
    }

    /**
     * Addressing mode: (indirect),Y
     *
     * Fetches a single byte from the address stored at (zero-page) + Y.
     * In case of a page boundary cross the cycle counter will be increased by one!
     */
    fun fetchIndirectIndexedYWithPC(): UByte {
        // get value from zero page (the destination address) and add Y as index
        return fetchAbsoluteY(fetchIndirectIndexedYAddressWithPC())
    }

    /**
     * Addressing mode: (indirect),Y
     *
     * Fetches an address stored in the zero page with respect to the zero page boundaries.
     * (handle zero page wrap in address (lo@00FF -> hi@0000 instead of hi@0100))
     *
     * http://www.emulator101.com/6502-addressing-modes.html
     * http://archive.6502.org/datasheets/mos_6501-6505_mpu_preliminary_aug_1975.pdf
     * https://stackoverflow.com/questions/46262435/indirect-y-indexed-addressing-mode-in-mos-6502
     */
    fun fetchIndirectIndexedYAddressWithPC(): Int {
        // get zero-page addr from 1st argument
        val zeroPageAddr = fetchWithPC().toInt()
        // ATTENTION FOR INDIRECT addressing: handle zero page wrap in address (lo@00FF -> hi@0000 instead of hi@0100)
        // get addr from zero-page
        val loByte = fetch(zeroPageAddr)
        // mask out hi-byte - use only the lower 8 address bits
        val hiByte = fetch((zeroPageAddr + 1) and 0xFF)
        return wordFromLoHi(loByte, hiByte)
    }

    /**
     * Returns the zero-page address from the first op argument.
     */
    fun fetchZeroPageAddressWithPC(): Int {
        return fetchWithPC().toInt()
    }

    /**
     * Returns the zero-page address from the first op argument indexed with X, with respect to the zero page boundaries.
     */
    fun fetchZeroPageXAddressWithPC(): Int {
        return (fetchWithPC().toInt() + registers.X.toInt()) and 0xFF
    }

    /**
     * Returns the zero-page address from the first op argument indexed with Y, with respect to the zero page boundaries.
     */
    fun fetchZeroPageYAddressWithPC(): Int {
        return (fetchWithPC().toInt() + registers.Y.toInt()) and 0xFF
    }

    /**
     * Fetches a single byte from the current PC and increases the PC by one.
     */
    fun fetchWithPC(): UByte {
        return fetch(registers.PC++)
    }

    /**
     * Fetches a word from the given address and returns it as Int.
     */
    fun fetchWord(address: Int): Int {
        // little-endian (Lo-Hi)
        return wordFromLoHi(fetch(address), fetch(address + 1))
    }

    /**
     * Fetches a word from the current PC and increases the PC by two.
     */
    fun fetchWordWithPC(): Int {
        val result = fetchWord(registers.PC++)
        registers.PC++
        return result
    }

    /**
     * Fetches a word indirect from the current PC and increases the PC by two.
     * Addressing mode: indirect
     */
    fun fetchWordIndirectWithPC(): Int {
        // fetch indirect address from op byte2 + byte3
        val indirectAddr = fetchWordWithPC()
        // fetch lo-byte from indirect address
        val lo = fetch(indirectAddr)
        // increase lo-byte of indirect address by 1 and ignore overflow ($FF+1 = $00)
        val loAddr = (loByteFromWord(indirectAddr) + 1u).toUByte()
        // get hi-byte of indirect address
        val hiAddr = hiByteFromWord(indirectAddr)
        // fetch hi-byte from increased indirect address
        val hi = fetch(wordFromLoHi(loAddr, hiAddr))
        return wordFromLoHi(lo, hi)
    }

    /**
     * Pops a word form the stack and increases the SP by two.
     */
    fun popWordFromStack(): Int {
        // little-endian (Lo-Hi)
        registers.SP++
        val lo = fetch(STACK_OFFSET + registers.SP.toInt())
        registers.SP++
        val hi = fetch(STACK_OFFSET + registers.SP.toInt())
        return wordFromLoHi(lo, hi)
    }

    /**
     * Stores a single byte at the given address.
     */
    fun store(address: Int, byte: UByte) {
        // todo: check address for access ram or rom
        // use only bit 0-15, mask out
        mem[address and 0xFFFF] = byte
    }

    /**
     * Stores a word in little-endian (hi-lo) at the given address.
     */
    @Suppress("unused")
    fun storeWord(address: Int, word: Int) {
        store(address, loByteFromWord(word))
        store(address + 1, hiByteFromWord(word))
    }

    /**
     * Pushes a single byte on the stack and decreases the SP by one.
     */
    fun pushToStack(byte: UByte) {
        store(STACK_OFFSET + registers.SP.toInt(), byte)
        registers.SP--
    }

    /**
     * Pops a single byte form the stack and increases the SP by one.
     */
    fun popFromStack(): UByte {
        registers.SP++
        return fetch(STACK_OFFSET + registers.SP.toInt())
    }

    /**
     * Pushes word on the stack and decreases the SP by two.
     */
    fun pushWordToStack(word: Int) {
        // little-endian (Lo-Hi)
        store(
            STACK_OFFSET + registers.SP.toInt(),
            hiByteFromWord(word)
        )
        registers.SP--
        store(
            STACK_OFFSET + registers.SP.toInt(),
            loByteFromWord(word)
        )
        registers.SP--
    }

    fun printMemoryLineWithAddress(address: Int, numBytes: Int = 8): String {
        return "${address.toUnprefixedHex()}: " + printMemoryLine(address, numBytes)
    }

    fun printMemoryLine(address: Int, numBytes: Int = 8): String {
        var memDump = ""
        for (i in 0..(numBytes-1)) {
            if (!memDump.isEmpty()) {
                memDump += " "
            }
            memDump += mem[address + i].toUnprefixedHex()
        }
        return memDump
    }

    @Suppress("unused")
    fun printStackLine(): String {
        return printMemoryLineWithAddress(STACK_OFFSET + registers.SP.toInt() + 1, 0xFF - registers.SP.toInt())
    }
}