package com.grupo5.clubdeportivo.ui

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.hasErrorText
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.grupo5.clubdeportivo.R
import com.grupo5.clubdeportivo.soporte.PruebaUi
import com.grupo5.clubdeportivo.util.Formato
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Módulo 2 · Búsqueda del socio, verificación de la cuota y cobro. Casos PR13, PR14 y PR15
 * del plan de pruebas y casos complementarios PR13b a PR15c.
 */
@RunWith(AndroidJUnit4::class)
class CobroCuotaTest {

    private val hoy = LocalDate.now()

    @Before
    fun prepararBaseYSesion() {
        PruebaUi.reiniciarBase()
        PruebaUi.iniciarSesionAdmin()
    }

    private fun abrirCobro(nroSocio: Int? = null): ActivityScenario<CobroCuotaActivity> {
        val intent = Intent(PruebaUi.contexto, CobroCuotaActivity::class.java)
        if (nroSocio != null) intent.putExtra(CobroCuotaActivity.EXTRA_NRO_SOCIO, nroSocio)
        return ActivityScenario.launch(intent)
    }

    private fun buscarPorNumero(nro: String) {
        onView(withId(R.id.etNroSocio)).perform(replaceText(nro), closeSoftKeyboard())
        onView(withId(R.id.btnBuscar)).perform(scrollTo(), click())
    }

    private fun registrarPagoConfirmando() {
        onView(withId(R.id.btnRegistrarPago)).perform(scrollTo(), click())
        onView(withText("REGISTRAR")).inRoot(isDialog()).perform(click())
    }

    /** PR13 · Búsqueda por tipo y número de documento de un socio con la cuota vencida. */
    @Test
    fun pr13_busquedaPorDocumento_muestraSocioInhabilitadoConDiasDeAtraso() {
        abrirCobro().use {
            onView(withId(R.id.rbPorDocumento)).perform(click())
            onView(withId(R.id.etNroDoc)).perform(replaceText("29555666"), closeSoftKeyboard())
            onView(withId(R.id.btnBuscar)).perform(scrollTo(), click())

            onView(withId(R.id.tvSocioNombre)).check(matches(withText("SOSA, Julio")))
            onView(withId(R.id.tvEstado)).check(matches(withText("INHABILITADO")))
            onView(withId(R.id.tvAvisoCuota)).check(matches(withText(containsString("14 día(s) de atraso"))))
            onView(withId(R.id.tvVencActual)).check(matches(withText(Formato.fecha(hoy.minusDays(14)))))
            // Cuota vencida: el nuevo período arranca hoy.
            onView(withId(R.id.tvNuevoVenc)).check(matches(withText(Formato.fecha(hoy.plusMonths(1)))))
        }
    }

    /** PR13b · Búsqueda por número de socio. */
    @Test
    fun pr13b_busquedaPorNumero_muestraAlSocio() {
        abrirCobro().use {
            buscarPorNumero("1")
            onView(withId(R.id.tvSocioNombre)).check(matches(withText("GOMEZ, Ana")))
            onView(withId(R.id.tvEstado)).check(matches(withText("HABILITADO")))
            onView(withId(R.id.etMonto)).check(matches(withText("18000")))
        }
    }

    /** PR13c · Socio inexistente: no se muestra la sección de cobro. */
    @Test
    fun pr13c_socioInexistente_noMuestraElCobro() {
        abrirCobro().use {
            buscarPorNumero("999")
            onView(withId(R.id.llCobro)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }

    /** PR13d · Buscar sin escribir el número: marca el campo. */
    @Test
    fun pr13d_busquedaVacia_marcaElCampo() {
        abrirCobro().use {
            onView(withId(R.id.btnBuscar)).perform(scrollTo(), click())
            onView(withId(R.id.etNroSocio)).check(matches(hasErrorText("Ingresá el número de socio")))
        }
    }

    /** PR14 · Las cuotas 3 y 6 solo se habilitan con tarjeta. */
    @Test
    fun pr14_cuotasSoloConTarjeta() {
        abrirCobro(nroSocio = 6).use {
            // EFECTIVO viene marcado por defecto.
            onView(withId(R.id.rbEfectivo)).check(matches(isChecked()))
            onView(withId(R.id.rbCuotas3)).check(matches(not(isEnabled())))
            onView(withId(R.id.rbCuotas6)).check(matches(not(isEnabled())))
            onView(withId(R.id.tvAvisoCuotas)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

            onView(withId(R.id.rbTarjeta)).perform(scrollTo(), click())
            onView(withId(R.id.rbCuotas3)).check(matches(isEnabled()))
            onView(withId(R.id.rbCuotas6)).check(matches(isEnabled()))
            onView(withId(R.id.tvAvisoCuotas)).check(matches(withEffectiveVisibility(Visibility.GONE)))

            // Si vuelve a efectivo, la cantidad vuelve a 1.
            onView(withId(R.id.rbCuotas6)).perform(scrollTo(), click())
            onView(withId(R.id.rbEfectivo)).perform(scrollTo(), click())
            onView(withId(R.id.rbCuotas1)).check(matches(isChecked()))
        }
    }

    /** PR14b · Pago con tarjeta en 6 cuotas: queda en el detalle del recibo. */
    @Test
    fun pr14b_pagoConTarjetaEnSeisCuotas() {
        abrirCobro(nroSocio = 6).use {
            onView(withId(R.id.rbTarjeta)).perform(scrollTo(), click())
            onView(withId(R.id.rbCuotas6)).perform(scrollTo(), click())
            registrarPagoConfirmando()

            onView(withId(R.id.tvDetalle)).check(matches(withText(containsString("Tarjeta de crédito · 6 cuota(s)"))))
            assertEquals(1, PruebaUi.contar(
                "SELECT COUNT(*) FROM cuota WHERE nro_socio = 6 AND forma_pago = 'TARJETA' AND cantidad_cuotas = 6"))
        }
    }

    /** PR15 · Pago adelantado: el nuevo vencimiento es el actual + 1 día + 1 mes. */
    @Test
    fun pr15_pagoAdelantado_noPierdeDias() {
        val vencimientoActual = hoy.plusDays(20)
        val inicio = vencimientoActual.plusDays(1)
        abrirCobro(nroSocio = 1).use {
            onView(withId(R.id.tvVencActual)).check(matches(withText(Formato.fecha(vencimientoActual))))
            onView(withId(R.id.tvNuevoVenc)).check(matches(withText(Formato.fecha(inicio.plusMonths(1)))))

            registrarPagoConfirmando()

            onView(withId(R.id.tvDetalle)).check(matches(withText(containsString(
                "Período: ${Formato.fecha(inicio)} al ${Formato.fecha(inicio.plusMonths(1))}"))))
            onView(withId(R.id.tvDetalle)).check(matches(not(withText(containsString("primera cuota")))))
        }
    }

    /** PR15b · Importe en cero: no se registra. */
    @Test
    fun pr15b_importeCero_marcaElImporte() {
        abrirCobro(nroSocio = 6).use {
            onView(withId(R.id.etMonto)).perform(scrollTo(), replaceText("0"), closeSoftKeyboard())
            onView(withId(R.id.btnRegistrarPago)).perform(scrollTo(), click())
            onView(withId(R.id.etMonto)).check(matches(hasErrorText("Ingresá un importe mayor a cero")))
            assertEquals(8, PruebaUi.contar("SELECT COUNT(*) FROM cuota"))
        }
    }

    /** PR15c · CANCELAR en la confirmación: no se registra nada. */
    @Test
    fun pr15c_cancelarConfirmacion_noRegistraElPago() {
        abrirCobro(nroSocio = 6).use {
            onView(withId(R.id.btnRegistrarPago)).perform(scrollTo(), click())
            onView(withText("CANCELAR")).inRoot(isDialog()).perform(click())
            onView(withId(R.id.tvSocioNombre)).check(matches(withText("SOSA, Julio")))
            assertEquals(8, PruebaUi.contar("SELECT COUNT(*) FROM cuota"))
        }
    }

    /** Control de acceso (M06): un empleado de consulta no puede cobrar. */
    @Test
    fun rolConsulta_noPuedeAbrirElCobro() {
        PruebaUi.iniciarSesionConsulta()
        abrirCobro().use { escenario ->
            assertEquals(Lifecycle.State.DESTROYED, escenario.state)
        }
    }
}
