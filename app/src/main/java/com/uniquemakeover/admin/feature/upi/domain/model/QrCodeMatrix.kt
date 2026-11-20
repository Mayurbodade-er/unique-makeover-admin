package com.uniquemakeover.admin.feature.upi.domain.model

data class QrCodeMatrix(
    val width: Int,
    val height: Int,
    val bits: BooleanArray
) {
    fun isBlack(x: Int, y: Int): Boolean = bits[y * width + x]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QrCodeMatrix
        if (width != other.width) return false
        if (height != other.height) return false
        return bits.contentEquals(other.bits)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + bits.contentHashCode()
        return result
    }
}
