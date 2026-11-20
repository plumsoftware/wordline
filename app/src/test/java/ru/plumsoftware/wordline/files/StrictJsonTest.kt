package ru.plumsoftware.wordline.files

import org.junit.Assert.*
import org.junit.Test

class StrictJsonTest {
    @Test fun canonicalOrderEscapingAndIntegers() {
        val value=strictJson("""{"z":-0,"a":[true,"а\nб",null],"nested":{"y":2,"x":1}}""".toByteArray())
        assertEquals("""{"a":[true,"а\nб",null],"nested":{"x":1,"y":2},"z":0}""",canonicalJson(value))
        assertEquals(canonicalJson(value),canonicalJson(strictJson(canonicalJson(value).toByteArray())))
    }
    @Test(expected=IllegalArgumentException::class) fun duplicateKeysAreRejected() {strictJson("""{"a":1,"a":2}""".toByteArray())}
    @Test(expected=IllegalArgumentException::class) fun deepNestingIsRejected() {strictJson(("{\"a\":"+"[".repeat(25)+"0"+"]".repeat(25)+"}").toByteArray())}
    @Test(expected=IllegalArgumentException::class) fun fractionalNumbersAreRejected() {strictJson("""{"a":1.2}""".toByteArray())}
    @Test(expected=IllegalArgumentException::class) fun unsafeIntegersAreRejected() {strictJson("""{"a":9007199254740992}""".toByteArray())}
    @Test(expected=IllegalArgumentException::class) fun unpairedSurrogateIsRejected() {strictJson("""{"a":"\ud800"}""".toByteArray())}
    @Test(expected=java.nio.charset.CharacterCodingException::class) fun malformedUtf8IsRejected() {strictJson(byteArrayOf(0x7b,0xc3.toByte(),0x28,0x7d))}
}
