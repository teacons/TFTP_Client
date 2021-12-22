package ru.fbear.tftp.client

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class MainKtTest {

    @Test
    fun toByteArray() {
        val num = 65534

        val expected = byteArrayOf(255.toByte(), 254.toByte())

        assertArrayEquals(expected, num.toByteArray())
    }
}