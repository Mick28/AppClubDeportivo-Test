package com.grupo5.clubdeportivo.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Prueba unitaria local (JVM) de la regla de negocio que define el estado del socio.
 *
 * Grupo PU-01 del plan de pruebas (ver PRUEBAS.md). Se usa una fecha fija como "hoy" para que
 * el resultado no dependa del día en que se ejecuta la prueba.
 */
class EstadoSocioTest {

    private val hoy = LocalDate.of(2026, 9, 30)

    @Test
    fun socioDadoDeBaja_quedaEnBaja_aunqueTengaLaCuotaAlDia() {
        val estado = EstadoSocio.calcular(
            aptoFisico = true, ultimoVencimiento = "2026-12-31", baja = true, hoy = hoy
        )
        assertEquals(EstadoSocio.BAJA, estado)
    }

    @Test
    fun socioSinNingunaCuotaPaga_quedaSinPrimeraCuota() {
        val estado = EstadoSocio.calcular(aptoFisico = true, ultimoVencimiento = null, baja = false, hoy = hoy)
        assertEquals(EstadoSocio.SIN_CUOTA, estado)
    }

    @Test
    fun cuotaQueVencioAyer_quedaInhabilitado() {
        val estado = EstadoSocio.calcular(true, hoy.minusDays(1).toString(), false, hoy)
        assertEquals(EstadoSocio.INHABILITADO, estado)
    }

    @Test
    fun cuotaQueVenceHoy_sigueHabilitado_porqueHoyEsSuUltimoDia() {
        val estado = EstadoSocio.calcular(true, hoy.toString(), false, hoy)
        assertEquals(EstadoSocio.HABILITADO, estado)
    }

    @Test
    fun cuotaAlDiaConAptoFisico_quedaHabilitado() {
        val estado = EstadoSocio.calcular(true, hoy.plusDays(20).toString(), false, hoy)
        assertEquals(EstadoSocio.HABILITADO, estado)
    }

    @Test
    fun cuotaAlDiaSinAptoFisico_quedaPendiente() {
        val estado = EstadoSocio.calcular(false, hoy.plusDays(9).toString(), false, hoy)
        assertEquals(EstadoSocio.PENDIENTE, estado)
    }

    @Test
    fun cuotaVencidaTienePrioridadSobreElAptoFisico() {
        // Sin apto y con la cuota vencida: manda la cuota, porque bloquea el ingreso igual.
        val estado = EstadoSocio.calcular(false, hoy.minusDays(14).toString(), false, hoy)
        assertEquals(EstadoSocio.INHABILITADO, estado)
    }

    @Test
    fun etiquetasQueSeMuestranEnPantalla() {
        assertEquals("HABILITADO", EstadoSocio.HABILITADO.etiqueta)
        assertEquals("INHABILITADO", EstadoSocio.INHABILITADO.etiqueta)
        assertEquals("PENDIENTE", EstadoSocio.PENDIENTE.etiqueta)
        assertEquals("SIN 1ª CUOTA", EstadoSocio.SIN_CUOTA.etiqueta)
        assertEquals("DADO DE BAJA", EstadoSocio.BAJA.etiqueta)
    }
}
