package com.grupo5.clubdeportivo.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.grupo5.clubdeportivo.model.EstadoSocio
import com.grupo5.clubdeportivo.soporte.PruebaUi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Prueba de instrumentación de la capa de datos (BaseDatosClub + DatosDePrueba) sobre la
 * base SQLite real del dispositivo. Grupo PI-01 del plan de pruebas (ver PRUEBAS.md).
 *
 * Los socios de prueba se insertan en este orden, por eso sus números son fijos:
 *   1 GOMEZ (vence en 20 días) · 2 RUIZ y 3 PEREZ (vencen hoy) · 4 DIAZ y 5 LOPEZ (vencieron ayer)
 *   6 SOSA (vencida hace 14 días) · 7 BENITEZ (hace 40 días) · 8 ACOSTA (sin apto, vence en 9 días)
 *   9 RUBIO (registrada sin primera cuota). Hay 8 cuotas cargadas: el próximo comprobante es el 9.
 */
@RunWith(AndroidJUnit4::class)
class BaseDatosClubTest {

    private lateinit var bd: BaseDatosClub
    private val hoy: LocalDate = LocalDate.now()

    @Before
    fun crearBaseLimpia() {
        PruebaUi.reiniciarBase()
        bd = BaseDatosClub(PruebaUi.contexto)
    }

    @After
    fun cerrarBase() = bd.close()

    private fun contar(sql: String, vararg args: String): Int =
        bd.readableDatabase.rawQuery(sql, args).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    private fun ejecutar(sql: String, vararg args: Any) = bd.writableDatabase.execSQL(sql, args)

    // =======================================================================================
    // Estructura y datos de prueba
    // =======================================================================================

    @Test
    fun onCreate_creaLasOchoTablasYElIndice() {
        val tablas = listOf("persona", "empleado", "socio", "cuota", "carnet", "actividad", "no_socio", "configuracion")
        tablas.forEach { tabla ->
            assertEquals("Falta la tabla $tabla", 1,
                contar("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?", tabla))
        }
        assertEquals(1, contar("SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' AND name = 'idx_cuota_socio_venc'"))
    }

    @Test
    fun datosDePrueba_cargaEmpleadosSociosCuotasYActividades() {
        assertEquals(2, contar("SELECT COUNT(*) FROM empleado"))
        assertEquals(9, contar("SELECT COUNT(*) FROM socio"))
        assertEquals(8, contar("SELECT COUNT(*) FROM cuota"))
        assertEquals(8, contar("SELECT COUNT(*) FROM carnet"))
        assertEquals(6, contar("SELECT COUNT(*) FROM actividad"))
    }

    @Test
    fun contrasenias_seGuardanHasheadasNuncaEnTextoPlano() {
        assertEquals(0, contar("SELECT COUNT(*) FROM empleado WHERE password_hash IN ('admin123','recep123')"))
        assertEquals(0, contar("SELECT COUNT(*) FROM socio WHERE password_hash = 'socio123'"))
        // Cada socio tiene su propio salt: nueve socios con la misma clave, nueve hashes distintos.
        assertEquals(9, contar("SELECT COUNT(DISTINCT password_hash) FROM socio"))
    }

    @Test
    fun onUpgrade_recreaLaBaseConLosDatosDePrueba() {
        ejecutar("DELETE FROM carnet WHERE nro_socio = 1")
        bd.onUpgrade(bd.writableDatabase, 1, 2)
        assertEquals(8, contar("SELECT COUNT(*) FROM carnet"))
        assertEquals(9, contar("SELECT COUNT(*) FROM socio"))
    }

    // =======================================================================================
    // Módulo 1 · Login
    // =======================================================================================

    @Test
    fun validarEmpleado_admin_devuelveElEmpleadoConRolAdmin() {
        val empleado = bd.validarEmpleado("admin", "admin123")
        assertNotNull(empleado)
        assertEquals("GONZALEZ", empleado!!.apellido)
        assertEquals("Emma", empleado.nombre)
        assertTrue(empleado.esAdministrador)
    }

    @Test
    fun validarEmpleado_recepcion_tieneRolConsulta() {
        val empleado = bd.validarEmpleado("recepcion", "recep123")
        assertNotNull(empleado)
        assertFalse(empleado!!.esAdministrador)
    }

    @Test
    fun validarEmpleado_claveIncorrectaOUsuarioInexistente_devuelveNull() {
        assertNull(bd.validarEmpleado("admin", "clave-erronea"))
        assertNull(bd.validarEmpleado("noexiste", "admin123"))
        assertNull(bd.validarEmpleado("ADMIN", "admin123")) // el usuario distingue mayúsculas
    }

    @Test
    fun validarEmpleado_empleadoInactivo_noPuedeIngresar() {
        ejecutar("UPDATE empleado SET activo = 0 WHERE usuario = 'recepcion'")
        assertNull(bd.validarEmpleado("recepcion", "recep123"))
    }

    @Test
    fun validarSocio_aliasYClaveCorrectos_devuelveSuNumeroDeSocio() {
        assertEquals(1, bd.validarSocio("agomez", "socio123"))
        assertEquals(9, bd.validarSocio("arubio", "socio123"))
    }

    @Test
    fun validarSocio_claveIncorrecta_devuelveNull() {
        assertNull(bd.validarSocio("agomez", "otra"))
        assertNull(bd.validarSocio("nadie", "socio123"))
    }

    @Test
    fun validarSocio_socioDadoDeBaja_noPuedeIngresar() {
        ejecutar("UPDATE socio SET baja = 1 WHERE alias = 'mruiz'")
        assertNull(bd.validarSocio("mruiz", "socio123"))
    }

    // =======================================================================================
    // Módulo 1 · Registro de socio
    // =======================================================================================

    @Test
    fun buscaAlias_cuentaLosSociosQueUsanEseAlias() {
        assertEquals(1, bd.buscaAlias("agomez"))
        assertEquals(0, bd.buscaAlias("alias.libre"))
    }

    @Test
    fun buscaDocumentoSocio_distingueTipoYNumero() {
        assertEquals(1, bd.buscaDocumentoSocio("DNI", "30111222"))
        assertEquals(0, bd.buscaDocumentoSocio("PAS", "30111222"))
        // La persona 20111222 es empleada, no socia.
        assertEquals(0, bd.buscaDocumentoSocio("DNI", "20111222"))
    }

    @Test
    fun insertarSocio_nuevo_naceInactivoSinCuotaYSinCarnet() {
        val nro = bd.insertarSocio("DNI", "40123456", "PRUEBA", "Tomás", "2000-01-15",
            "11 1234-5678", "tprueba@mail.com", true, "tprueba", "clave123")

        assertEquals(10L, nro)
        assertEquals(0, contar("SELECT estado_activo FROM socio WHERE nro_socio = 10"))
        assertNull(bd.obtenerCarnet(10))
        val socio = bd.buscarSocioPorNumero(10)!!
        assertNull(socio.ultimoVencimiento)
        assertEquals(EstadoSocio.SIN_CUOTA, socio.estado())
        assertEquals(10, bd.validarSocio("tprueba", "clave123"))
    }

    @Test
    fun insertarSocio_aliasRepetido_fallaYNoDejaUnaPersonaSuelta() {
        val nro = bd.insertarSocio("DNI", "40111222", "REPETIDO", "Alias", "2000-01-15",
            "11 1234-5678", "", true, "agomez", "clave123")

        assertEquals(-1L, nro)
        // La transacción se deshizo completa: tampoco quedó la persona.
        assertEquals(0, contar("SELECT COUNT(*) FROM persona WHERE nro_documento = '40111222'"))
    }

    @Test
    fun insertarSocio_personaQueYaExistia_reutilizaSuRegistro() {
        // Emma (empleada) se asocia al club: no se duplica la persona.
        val nro = bd.insertarSocio("DNI", "20111222", "GONZALEZ", "Emma", "1980-03-12",
            "11 4000-1000", "emma@mail.com", true, "egonzalez", "clave123")

        assertTrue(nro > 0)
        assertEquals(1, contar("SELECT COUNT(*) FROM persona WHERE tipo_documento = 'DNI' AND nro_documento = '20111222'"))
        assertEquals(1, bd.buscaDocumentoSocio("DNI", "20111222"))
    }

    @Test
    fun registrarAptoFisico_pasaDePendienteAHabilitado() {
        assertEquals(EstadoSocio.PENDIENTE, bd.buscarSocioPorNumero(8)!!.estado())
        assertTrue(bd.registrarAptoFisico(8))
        assertEquals(EstadoSocio.HABILITADO, bd.buscarSocioPorNumero(8)!!.estado())
    }

    @Test
    fun registrarAptoFisico_socioInexistente_devuelveFalse() {
        assertFalse(bd.registrarAptoFisico(999))
    }

    // =======================================================================================
    // Módulo 2 · Búsqueda y cobro de cuota
    // =======================================================================================

    @Test
    fun buscarSocio_porNumeroYPorDocumento_devuelvenElMismoSocio() {
        val porNumero = bd.buscarSocioPorNumero(6)
        val porDocumento = bd.buscarSocioPorDocumento("DNI", "29555666")
        assertNotNull(porNumero)
        assertEquals(porNumero, porDocumento)
        assertEquals("SOSA, Julio", porNumero!!.apellidoYNombre)
        assertEquals(hoy.minusDays(14).toString(), porNumero.ultimoVencimiento)
        assertEquals(EstadoSocio.INHABILITADO, porNumero.estado())
    }

    @Test
    fun buscarSocio_inexistente_devuelveNull() {
        assertNull(bd.buscarSocioPorNumero(999))
        assertNull(bd.buscarSocioPorDocumento("DNI", "11111111"))
    }

    @Test
    fun obtenerValorCuota_tomaElValorDeLaTablaConfiguracion() {
        assertEquals(18000.0, bd.obtenerValorCuota(), 0.001)
        ejecutar("UPDATE configuracion SET valor = '21500' WHERE clave = ?", BaseDatosClub.CLAVE_VALOR_CUOTA)
        assertEquals(21500.0, bd.obtenerValorCuota(), 0.001)
    }

    @Test
    fun calcularPeriodo_primeraCuotaOVencida_arrancaHoyYVenceEnUnMes() {
        val esperado = Pair(hoy, hoy.plusMonths(1))
        assertEquals(esperado, bd.calcularPeriodo(null))
        assertEquals(esperado, bd.calcularPeriodo(hoy.minusDays(14).toString()))
    }

    @Test
    fun calcularPeriodo_pagoAdelantado_arrancaElDiaSiguienteAlVencimiento() {
        val vigente = hoy.plusDays(20)
        assertEquals(Pair(vigente.plusDays(1), vigente.plusDays(1).plusMonths(1)), bd.calcularPeriodo(vigente.toString()))
        // Si vence hoy todavía no está vencida: el nuevo período empieza mañana.
        assertEquals(Pair(hoy.plusDays(1), hoy.plusDays(1).plusMonths(1)), bd.calcularPeriodo(hoy.toString()))
    }

    @Test
    fun registrarPago_primeraCuota_emiteElCarnetYHabilitaAlSocio() {
        val resultado = bd.registrarPago(9, idEmpleado = 1, monto = 18000.0, formaPago = "EFECTIVO", cantidadCuotas = 1)

        assertNotNull(resultado)
        assertEquals(9L, resultado!!.nroComprobante)
        assertTrue(resultado.carnetEmitido)
        assertEquals(hoy.toString(), resultado.fechaInicio)
        assertEquals(hoy.plusMonths(1).toString(), resultado.fechaVencimiento)
        assertEquals(1, contar("SELECT estado_activo FROM socio WHERE nro_socio = 9"))

        val carnet = bd.obtenerCarnet(9)
        assertNotNull(carnet)
        assertEquals(hoy.toString(), carnet!!.fechaEmision)
        assertEquals(hoy.plusMonths(12).toString(), carnet.fechaVencimientoCarnet)
        // RUBIO no presentó apto físico: pagó pero queda PENDIENTE.
        assertEquals(EstadoSocio.PENDIENTE, carnet.socio.estado())
    }

    @Test
    fun registrarPago_adelantado_extiendeElPeriodoSinPerderDias() {
        val resultado = bd.registrarPago(1, 1, 18000.0, "TARJETA", 3)!!
        val inicio = hoy.plusDays(21)
        assertEquals(inicio.toString(), resultado.fechaInicio)
        assertEquals(inicio.plusMonths(1).toString(), resultado.fechaVencimiento)
        assertFalse("GOMEZ ya tenía carnet", resultado.carnetEmitido)
    }

    @Test
    fun registrarPago_cuotaVencida_reactivaAlSocio() {
        assertEquals(0, contar("SELECT estado_activo FROM socio WHERE nro_socio = 6"))
        val resultado = bd.registrarPago(6, 1, 18000.0, "EFECTIVO", 1)!!
        assertEquals(hoy.plusMonths(1).toString(), resultado.fechaVencimiento)
        assertEquals(1, contar("SELECT estado_activo FROM socio WHERE nro_socio = 6"))
        assertEquals(EstadoSocio.HABILITADO, bd.buscarSocioPorNumero(6)!!.estado())
    }

    @Test
    fun registrarPago_carnetVencido_loRenueva() {
        ejecutar("UPDATE carnet SET fecha_vencimiento = ? WHERE nro_socio = 6", hoy.minusDays(1).toString())
        val resultado = bd.registrarPago(6, 1, 18000.0, "EFECTIVO", 1)!!

        assertFalse(resultado.carnetEmitido)
        val carnet = bd.obtenerCarnet(6)!!
        assertEquals(hoy.toString(), carnet.fechaEmision)
        assertEquals(hoy.plusMonths(12).toString(), carnet.fechaVencimientoCarnet)
    }

    @Test
    fun registrarPago_efectivoEnTresCuotas_esRechazadoPorLaBase() {
        assertNull(bd.registrarPago(6, 1, 18000.0, "EFECTIVO", 3))
        assertEquals("No se debe guardar ninguna cuota", 8, contar("SELECT COUNT(*) FROM cuota"))
        assertEquals(0, contar("SELECT estado_activo FROM socio WHERE nro_socio = 6"))
    }

    @Test
    fun registrarPago_montoCeroOSocioInexistente_devuelveNull() {
        assertNull(bd.registrarPago(6, 1, 0.0, "EFECTIVO", 1))
        assertNull(bd.registrarPago(999, 1, 18000.0, "EFECTIVO", 1))
        assertEquals(8, contar("SELECT COUNT(*) FROM cuota"))
    }

    @Test
    fun registrarPago_guardaElEmpleadoQueCobro() {
        bd.registrarPago(6, idEmpleado = 1, monto = 18000.0, formaPago = "EFECTIVO", cantidadCuotas = 1)
        assertEquals(1, contar("SELECT COUNT(*) FROM cuota WHERE nro_cuota = 9 AND id_empleado = 1 AND nro_socio = 6"))
    }

    // =======================================================================================
    // Control automático de vencimientos (HU08 / M09)
    // =======================================================================================

    @Test
    fun inhabilitarVencidos_soloAfectaALosQueVencieronAntesDeLaFecha() {
        assertEquals("Los datos de prueba ya nacen con el estado correcto", 0, bd.inhabilitarVencidos(hoy))
        // Mañana, RUIZ y PEREZ (que vencen hoy) pasan a inhabilitados.
        assertEquals(2, bd.inhabilitarVencidos(hoy.plusDays(1)))
        assertEquals(0, contar("SELECT estado_activo FROM socio WHERE nro_socio = 2"))
        assertEquals("GOMEZ sigue al día", 1, contar("SELECT estado_activo FROM socio WHERE nro_socio = 1"))
    }

    @Test
    fun inhabilitarVencidos_socioActivoSinNingunaCuota_tambienSeInhabilita() {
        ejecutar("UPDATE socio SET estado_activo = 1 WHERE nro_socio = 9") // RUBIO, sin cuotas
        assertEquals(1, bd.inhabilitarVencidos(hoy))
        assertEquals(0, contar("SELECT estado_activo FROM socio WHERE nro_socio = 9"))
    }

    // =======================================================================================
    // Módulo 3 · Listado diario de vencimientos
    // =======================================================================================

    @Test
    fun listarVencimientosDelDia_vencenHoyOVencieronAyer_ordenadosPorApellido() {
        val lista = bd.listarVencimientosDelDia(hoy)

        assertEquals(listOf("DIAZ", "LOPEZ", "PEREZ", "RUIZ"), lista.map { it.apellido })
        assertEquals(listOf(4, 5, 3, 2), lista.map { it.id })
        assertEquals(hoy.minusDays(1).toString(), lista[0].fechaVencimiento)
        assertEquals(hoy.toString(), lista[3].fechaVencimiento)
        assertEquals("11 3333-2222", lista[0].telefono)
    }

    @Test
    fun listarVencimientosDelDia_socioQueYaRenovo_noAparece() {
        bd.registrarPago(5, 1, 18000.0, "EFECTIVO", 1) // LOPEZ paga
        val apellidos = bd.listarVencimientosDelDia(hoy).map { it.apellido }
        assertEquals(listOf("DIAZ", "PEREZ", "RUIZ"), apellidos)
    }

    @Test
    fun listarVencimientosDelDia_socioDadoDeBaja_noAparece() {
        ejecutar("UPDATE socio SET baja = 1 WHERE nro_socio = 2") // RUIZ
        assertEquals(listOf("DIAZ", "LOPEZ", "PEREZ"), bd.listarVencimientosDelDia(hoy).map { it.apellido })
        assertEquals(1, bd.contarVencenHoy(hoy))
    }

    @Test
    fun listarVencidasAcumuladas_delMasAtrasadoAlMasReciente() {
        val lista = bd.listarVencidasAcumuladas(hoy)
        assertEquals(listOf("BENITEZ", "SOSA"), lista.map { it.apellido })
        assertEquals(hoy.minusDays(40).toString(), lista[0].fechaVencimiento)
    }

    @Test
    fun contarVencenHoy_cuentaSoloLosQueVencenEseDia() {
        assertEquals(2, bd.contarVencenHoy(hoy))                 // RUIZ y PEREZ
        assertEquals(1, bd.contarVencenHoy(hoy.plusDays(20)))    // GOMEZ
        assertEquals(0, bd.contarVencenHoy(hoy.plusDays(2)))
    }
}
