package com.grupo5.clubdeportivo.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.grupo5.clubdeportivo.R
import com.grupo5.clubdeportivo.soporte.PruebaUi
import com.grupo5.clubdeportivo.util.Formato
import com.grupo5.clubdeportivo.util.Sesion
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Flujo completo de alta de un socio, de punta a punta (Módulos 1 y 2):
 * registro → cobro automático de la 1ª cuota → recibo → carnet → ingreso del socio nuevo.
 * Casos PR09, PR10, PR11 y PR12 del plan de pruebas.
 */
@RunWith(AndroidJUnit4::class)
class FlujoAltaSocioTest {

    private val hoy = LocalDate.now()
    private val vencimiento = hoy.plusMonths(1)

    @Before
    fun prepararBaseYSesion() {
        PruebaUi.reiniciarBase()
        PruebaUi.iniciarSesionAdmin()
    }

    private fun registrarSocioNuevo() {
        onView(withId(R.id.etNroDoc)).perform(scrollTo(), replaceText("40123456"))
        onView(withId(R.id.etApellido)).perform(scrollTo(), replaceText("Prueba"))
        onView(withId(R.id.etNombre)).perform(scrollTo(), replaceText("tomás"), closeSoftKeyboard())
        RegistroSocioTest.elegirFechaDeNacimiento(2000, 1, 15)
        onView(withId(R.id.etTelefono)).perform(scrollTo(), replaceText("11 1234-5678"))
        onView(withId(R.id.etEmail)).perform(scrollTo(), replaceText("tprueba@mail.com"))
        onView(withId(R.id.cbAptoFisico)).perform(scrollTo(), click())
        onView(withId(R.id.etAlias)).perform(scrollTo(), replaceText("tprueba"))
        onView(withId(R.id.etClave)).perform(scrollTo(), replaceText("clave123"))
        onView(withId(R.id.etClaveRepetida)).perform(scrollTo(), replaceText("clave123"), closeSoftKeyboard())
        onView(withId(R.id.btnGuardar)).perform(scrollTo(), click())
    }

    @Test
    fun pr09_a_pr12_altaCompleta_cobro_recibo_carnet_eIngresoDelSocio() {
        ActivityScenario.launch(RegistroSocioActivity::class.java).use {
            // ---- PR09 · Alta completa: pasa solo al cobro de la primera cuota ----
            registrarSocioNuevo()

            onView(withId(R.id.tvTituloBarra)).check(matches(withText("PRIMERA CUOTA")))
            onView(withId(R.id.llBusqueda)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.tvSocioNombre)).check(matches(withText("PRUEBA, Tomás")))
            onView(withId(R.id.tvSocioDatos)).check(matches(withText("Socio N° 10 · DNI 40123456")))
            onView(withId(R.id.tvAvisoCuota)).check(matches(withText(containsString("Primera cuota del socio"))))
            onView(withId(R.id.etMonto)).check(matches(withText("18000")))
            onView(withId(R.id.tvVencActual)).check(matches(withText("Sin cuotas")))
            onView(withId(R.id.tvNuevoVenc)).check(matches(withText(Formato.fecha(vencimiento))))
            assertEquals(1, PruebaUi.contar("SELECT COUNT(*) FROM socio WHERE alias = 'tprueba' AND estado_activo = 0"))

            // ---- PR10 · Cobro de la 1ª cuota en efectivo → recibo con los 7 datos ----
            onView(withId(R.id.btnRegistrarPago)).perform(scrollTo(), click())
            onView(withText("REGISTRAR")).inRoot(isDialog()).perform(click())

            onView(withId(R.id.tvNombreDocumento)).check(matches(withText("RECIBO DE PAGO")))
            onView(withId(R.id.tvNroDocumento)).check(matches(withText("N° 000009")))
            onView(withId(R.id.tvFechaDocumento)).check(matches(withText("Fecha: ${Formato.fecha(hoy)}")))
            onView(withId(R.id.tvEmisor)).check(matches(withText(containsString("Atendió: GONZALEZ, Emma"))))
            onView(withId(R.id.tvReceptor)).check(matches(withText(containsString("PRUEBA, Tomás"))))
            onView(withId(R.id.tvDetalle)).check(matches(withText(containsString("(primera cuota)"))))
            onView(withId(R.id.tvDetalle)).check(matches(withText(containsString(
                "Período: ${Formato.fecha(hoy)} al ${Formato.fecha(vencimiento)}"))))
            onView(withId(R.id.tvMonto)).check(matches(withText(Formato.moneda(18000.0))))
            onView(withId(R.id.tvEstadoResultante)).check(matches(withText(containsString("HABILITADO"))))
            onView(withId(R.id.tvEstadoResultante)).check(matches(withText(containsString("Se emitió el carnet"))))
            assertEquals(1, PruebaUi.contar("SELECT COUNT(*) FROM carnet WHERE nro_socio = 10"))

            // ---- PR11 · Carnet visible para el empleado después del alta ----
            onView(withId(R.id.btnVerCarnet)).perform(scrollTo(), click())

            onView(withId(R.id.tvApellidoCarnet)).check(matches(withText("PRUEBA")))
            onView(withId(R.id.tvNombreCarnet)).check(matches(withText("Tomás")))
            onView(withId(R.id.tvNroSocioCarnet)).check(matches(withText("0010")))
            onView(withId(R.id.tvVencCuotaCarnet)).check(matches(withText(Formato.fecha(vencimiento))))
            onView(withId(R.id.tvEstadoCarnet)).check(matches(withText("HABILITADO")))
        }

        // ---- PR12 · El socio nuevo entra a la app con su alias y ve su carnet ----
        PruebaUi.cerrarTodasLasActividades()
        Sesion.cerrar()
        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(withId(R.id.rbSocio)).perform(click())
            onView(withId(R.id.etUsuario)).perform(replaceText("tprueba"))
            onView(withId(R.id.etClave)).perform(replaceText("clave123"), closeSoftKeyboard())
            onView(withId(R.id.btnIngresar)).perform(scrollTo(), click())

            onView(withId(R.id.tvBienvenidaSocio)).check(matches(withText("Hola, Tomás")))
            onView(withId(R.id.tvApellidoCarnet)).check(matches(withText("PRUEBA")))
            onView(withId(R.id.tvEstadoCarnet)).check(matches(withText("HABILITADO")))
            assertEquals(10, Sesion.nroSocio)
        }
    }
}
