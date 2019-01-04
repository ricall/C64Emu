package c64.emulation.vic

/**
 * Emulation of the C64 video chip VIC-II - MOS 6567/6569.
 *
 * @author Daniel Schulte 2017-2019
 */
class VIC {

    // TODO: handle text-mode
    // TODO: handle bitmap-mode
    // TODO: handle rasterline in $D012 (+$D011 bit 7)
    // TODO: handle interrupt status in $D019
    // TODO: handle background color at $D021
    // TODO: handle border color at $D020
    // TODO: registers are from $D000-D3FF, repeating every 64bytes 16x ($D000, $D040, $D080,...)
    // TODO: handle screen-memory AND charset (in current video-bank) in $D018
    // TODO: color ram always at $D800
    // TODO: set used video-bank (bit 0+1) in $DD00 (CIA2) (+$DD02 Port A data direction register)
}