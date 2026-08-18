package com.stronk.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testy generatora kodu dostępu — kod jest tożsamością danych, więc musi
 * być nietrywialny (długość, alfabet bez mylących znaków, losowość).
 */
class AccessCodeGeneratorTest {

    @Test
    fun `kod ma dokładnie 8 znaków`() {
        repeat(100) {
            assertEquals(AccessCodeGenerator.CODE_LENGTH, AccessCodeGenerator.generate().length)
        }
    }

    @Test
    fun `kod używa wyłącznie znaków z alfabetu`() {
        repeat(100) {
            AccessCodeGenerator.generate().forEach { char ->
                assertTrue("znak $char spoza alfabetu", char in AccessCodeGenerator.ALPHABET)
            }
        }
    }

    @Test
    fun `alfabet nie zawiera mylących znaków O 0 I 1 L`() {
        for (confusing in "O0I1L") {
            assertFalse("alfabet zawiera mylący znak $confusing", confusing in AccessCodeGenerator.ALPHABET)
        }
    }

    @Test
    fun `kody w próbie 1000 są unikalne`() {
        // 31^8 ≈ 8,5×10^11 kombinacji — kolizja w 1000 próbach praktycznie niemożliwa
        val codes = (1..1000).map { AccessCodeGenerator.generate() }.toSet()
        assertEquals(1000, codes.size)
    }

    @Test
    fun `normalize usuwa białe znaki i podnosi litery`() {
        assertEquals("K7MPQ2XW", AccessCodeGenerator.normalize(" k7mp q2xw\t"))
    }

    @Test
    fun `isValid akceptuje poprawny kod`() {
        assertTrue(AccessCodeGenerator.isValid("K7MPQ2XW"))
        assertTrue(AccessCodeGenerator.isValid(AccessCodeGenerator.generate()))
    }

    @Test
    fun `isValid odrzuca złą długość i znaki spoza alfabetu`() {
        assertFalse("za krótki", AccessCodeGenerator.isValid("K7MPQ2X"))
        assertFalse("za długi", AccessCodeGenerator.isValid("K7MPQ2XWA"))
        assertFalse("pusty", AccessCodeGenerator.isValid(""))
        assertFalse("mylący znak O", AccessCodeGenerator.isValid("K7MPQ2XO"))
        assertFalse("cyfra 0", AccessCodeGenerator.isValid("K7MPQ2X0"))
        assertFalse("mała litera", AccessCodeGenerator.isValid("k7mpq2xw"))
    }
}
