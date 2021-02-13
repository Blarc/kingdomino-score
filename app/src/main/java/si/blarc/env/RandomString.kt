package si.blarc.env

import java.util.*

class RandomString(length: Int, random: Random?, symbols: String) {
    /**
     * Generate a random string.
     */
    fun nextString(): String {
        for (idx in buf.indices) buf[idx] = symbols[random.nextInt(symbols.size)]
        return String(buf)
    }

    private val random: Random
    private val symbols: CharArray
    private val buf: CharArray

    /**
     * Create an alphanumeric string generator.
     */
    companion object {
        private const val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private const val digits = "0123456789"
        const val alphanumericUpper = upper + digits
    }

    init {
        require(length >= 1)
        require(symbols.length >= 2)
        this.random = Objects.requireNonNull(random)!!
        this.symbols = symbols.toCharArray()
        buf = CharArray(length)
    }
}