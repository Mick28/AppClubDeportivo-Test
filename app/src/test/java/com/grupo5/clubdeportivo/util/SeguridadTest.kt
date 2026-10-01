package com.grupo5.clubdeportivo.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias locales del hash de contraseñas con salt (mejora M07). Grupo PU-04.
 */
class SeguridadTest {

    @Test
    fun generarSalt_devuelve16BytesEnHexadecimal() {
        val salt = Seguridad.generarSalt()
        assertEquals(32, salt.length)
        assertTrue(salt.matches(Regex("[0-9a-f]{32}")))
    }

    @Test
    fun generarSalt_esAleatorio() {
        val salts = (1..50).map { Seguridad.generarSalt() }.toSet()
        assertEquals("No debería repetirse ningún salt", 50, salts.size)
    }

    @Test
    fun hashear_esSha256DeSaltMasClave() {
        // Vector conocido de SHA-256: SHA-256("abc").
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Seguridad.hashear(clave = "c", salt = "ab")
        )
    }

    @Test
    fun hashear_mismaClaveYMismoSalt_daElMismoHash() {
        val salt = Seguridad.generarSalt()
        assertEquals(Seguridad.hashear("admin123", salt), Seguridad.hashear("admin123", salt))
    }

    @Test
    fun hashear_mismaClaveConDistintoSalt_daDistintoHash() {
        // Es la mejora respecto de DSOO: dos usuarios con la misma clave no comparten el hash.
        val hash1 = Seguridad.hashear("socio123", Seguridad.generarSalt())
        val hash2 = Seguridad.hashear("socio123", Seguridad.generarSalt())
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun hashear_nuncaDevuelveLaClaveEnTextoPlano() {
        val hash = Seguridad.hashear("admin123", "0011")
        assertEquals(64, hash.length)
        assertTrue(!hash.contains("admin123"))
    }
}
