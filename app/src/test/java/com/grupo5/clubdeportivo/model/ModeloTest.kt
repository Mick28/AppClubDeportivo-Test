package com.grupo5.clubdeportivo.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Pruebas unitarias locales de las clases de modelo (Socio, SocioDetalle y Empleado).
 * Grupo PU-02 del plan de pruebas (ver PRUEBAS.md).
 */
class ModeloTest {

    // ---------------------------------------------------------------------------------------
    // Socio (data class del RecyclerView, Módulo 3)
    // ---------------------------------------------------------------------------------------

    @Test
    fun socio_apellidoYNombre_llevaElApellidoEnMayusculas() {
        val socio = Socio(id = 2, apellido = "Ruiz", nombre = "Marta", fechaVencimiento = "2026-09-30")
        assertEquals("RUIZ, Marta", socio.apellidoYNombre)
    }

    @Test
    fun socio_telefonoYEmailSonOpcionales() {
        val socio = Socio(1, "GOMEZ", "Ana", "2026-10-20")
        assertEquals("", socio.telefono)
        assertEquals("", socio.email)
    }

    @Test
    fun socio_esDataClass_dosSociosConLosMismosDatosSonIguales() {
        val a = Socio(3, "PEREZ", "Luis", "2026-09-30", "11 4444-3333")
        val b = Socio(3, "PEREZ", "Luis", "2026-09-30", "11 4444-3333")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    // ---------------------------------------------------------------------------------------
    // SocioDetalle (cobro de cuota y carnet, Módulo 2)
    // ---------------------------------------------------------------------------------------

    private fun detalle(apto: Boolean, vencimiento: String?, baja: Boolean = false) = SocioDetalle(
        nroSocio = 6, tipoDocumento = "DNI", nroDocumento = "29555666",
        apellido = "Sosa", nombre = "Julio", telefono = "11 6666-7777", email = "jsosa@mail.com",
        fechaInscripcion = "2026-08-16", aptoFisico = apto, baja = baja, ultimoVencimiento = vencimiento
    )

    @Test
    fun socioDetalle_documento_uneTipoYNumero() {
        assertEquals("DNI 29555666", detalle(true, null).documento)
        assertEquals("SOSA, Julio", detalle(true, null).apellidoYNombre)
    }

    @Test
    fun socioDetalle_estado_delegaEnLaReglaDeEstadoSocio() {
        val hoy = LocalDate.of(2026, 9, 30)
        assertEquals(EstadoSocio.INHABILITADO, detalle(true, "2026-09-16").estado(hoy))
        assertEquals(EstadoSocio.HABILITADO, detalle(true, "2026-10-20").estado(hoy))
        assertEquals(EstadoSocio.PENDIENTE, detalle(false, "2026-10-20").estado(hoy))
        assertEquals(EstadoSocio.SIN_CUOTA, detalle(true, null).estado(hoy))
        assertEquals(EstadoSocio.BAJA, detalle(true, "2026-10-20", baja = true).estado(hoy))
    }

    // ---------------------------------------------------------------------------------------
    // Empleado (control de acceso por rol, mejora M06)
    // ---------------------------------------------------------------------------------------

    @Test
    fun empleado_conRolAdmin_esAdministrador() {
        val admin = Empleado(1, "admin", "GONZALEZ", "Emma", Empleado.ROL_ADMIN)
        assertTrue(admin.esAdministrador)
    }

    @Test
    fun empleado_conRolConsulta_noEsAdministrador() {
        val recepcion = Empleado(2, "recepcion", "MORENO", "Diego", Empleado.ROL_CONSULTA)
        assertFalse(recepcion.esAdministrador)
    }
}
