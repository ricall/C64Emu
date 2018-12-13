package c64.util

/**
 * @author schulted 2017-2018
 */

/**
 * Returns the given Int as printed hex string in format $AAAA.
 */
fun Int.toHex(): String {
    return "$${this.toString(16).toUpperCase().padStart(4, '0')}"
}

/**
 * Returns the given Int as printed hex string in format AAAA.
 */
fun Int.toUnprefixedHex(): String {
    return this.toString(16).toUpperCase().padStart(4, '0')
}

/**
 * Returns the given UByte as printed hex string in format $AA.
 */
@ExperimentalUnsignedTypes
fun UByte.toHex(): String {
    return "$${this.toString(16).toUpperCase().padStart(2, '0')}"
}

/**
 * Returns the given UByte as printed hex string in format AA.
 */
@ExperimentalUnsignedTypes
fun UByte.toUnprefixedHex(): String {
    return this.toString(16).toUpperCase().padStart(2, '0')
}

/**
 * Returns the boolean represented as a bit-string, "0" or "1".
 */
fun Boolean.toBitString(): String {
    return if (this) "1" else "0"
}

/**
 * Increments the UByte by the given value and returns a new UByte.
 */
@ExperimentalUnsignedTypes
fun UByte.incBy(value: UByte): UByte {
    return this.plus(value).toUByte()
}

/**
 * Decrements the UByte by the given value and returns a new UByte.
 */
@ExperimentalUnsignedTypes
fun UByte.decBy(value: UByte): UByte {
    return this.minus(value).toUByte()
}
