package com.grupo5.clubdeportivo.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Pruebas unitarias locales de las funciones de formato que se ven en pantalla
 * (fechas, importes, número de comprobante y saludo). Grupo PU-03 del plan de pruebas.
 */
class FormatoTest {

    @Test
    fun fecha_convierteDeIsoADiaMesAnio() {
        assertEquals("27/09/2026", Formato.fecha("2026-09-27"))
        assertEquals("01/01/2027", Formato.fecha(LocalDate.of(2027, 1, 1)))
    }

    @Test
    fun fecha_nulaOVacia_muestraUnGuion() {
        assertEquals("—", Formato.fecha(null as String?))
        assertEquals("—", Formato.fecha("   "))
    }

    @Test
    fun fecha_textoQueNoEsFecha_seDevuelveSinCambios() {
        assertEquals("sin fecha", Formato.fecha("sin fecha"))
    }

    @Test
    fun fechaLarga_incluyeElDiaDeLaSemanaConMayusculaInicial() {
        assertEquals("Miércoles 30 de septiembre de 2026", Formato.fechaLarga(LocalDate.of(2026, 9, 30)))
    }

    @Test
    fun moneda_usaFormatoArgentinoSinDecimalesCuandoSonCero() {
        assertEquals("$ 18.000", Formato.moneda(18000.0))
        assertEquals("$ 500", Formato.moneda(500.0))
    }

    @Test
    fun moneda_conCentavos_muestraDosDecimalesConComa() {
        assertEquals("$ 18.000,50", Formato.moneda(18000.5))
    }

    @Test
    fun nroComprobante_completaConCerosHastaSeisDigitos() {
        assertEquals("000009", Formato.nroComprobante(9))
        assertEquals("123456", Formato.nroComprobante(123456))
    }

    @Test
    fun saludo_segunLaHoraDelDia_limitesIncluidos() {
        assertEquals("Buenas noches", Formato.saludo(LocalTime.of(5, 59)))
        assertEquals("Buenos días", Formato.saludo(LocalTime.of(6, 0)))
        assertEquals("Buenos días", Formato.saludo(LocalTime.of(12, 59)))
        assertEquals("Buenas tardes", Formato.saludo(LocalTime.of(13, 0)))
        assertEquals("Buenas tardes", Formato.saludo(LocalTime.of(19, 59)))
        assertEquals("Buenas noches", Formato.saludo(LocalTime.of(20, 0)))
    }
}
