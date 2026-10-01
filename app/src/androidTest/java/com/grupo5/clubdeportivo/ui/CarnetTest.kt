package com.grupo5.clubdeportivo.ui

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.grupo5.clubdeportivo.R
import com.grupo5.clubdeportivo.soporte.PruebaUi
import com.grupo5.clubdeportivo.util.Sesion
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Módulo 2 · Carnet digital en sus dos modos (empleado y socio). Caso PR19 del plan de
 * pruebas y casos complementarios PR19b a PR19f.
 */
@RunWith(AndroidJUnit4::class)
class CarnetTest {

    @Before
    fun prepararBase() = PruebaUi.reiniciarBase()

    private fun abrirCarnet(nroSocio: Int, modo: String): ActivityScenario<CarnetActivity> {
        val intent = Intent(PruebaUi.contexto, CarnetActivity::class.java)
            .putExtra(CarnetActivity.EXTRA_NRO_SOCIO, nroSocio)
            .putExtra(CarnetActivity.EXTRA_MODO, modo)
        return ActivityScenario.launch(intent)
    }

    /** PR19 · Apto físico pendiente: el administrador lo registra y el socio pasa a HABILITADO. */
    @Test
    fun pr19_registrarAptoFisico_pasaDePendienteAHabilitado() {
        PruebaUi.iniciarSesionAdmin()
        abrirCarnet(8, CarnetActivity.MODO_EMPLEADO).use {
            onView(withId(R.id.tvApellidoCarnet)).check(matches(withText("ACOSTA")))
            onView(withId(R.id.tvEstadoCarnet)).check(matches(withText("PENDIENTE")))

            onView(withId(R.id.btnRegistrarApto)).perform(scrollTo(), click())
            onView(withText("CONFIRMAR")).inRoot(isDialog()).perform(click())

            onView(withId(R.id.tvEstadoCarnet)).check(matches(withText("HABILITADO")))
            onView(withId(R.id.btnRegistrarApto)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            assertEquals(1, PruebaUi.contar("SELECT apto_fisico FROM socio WHERE nro_socio = 8"))
        }
    }

    /** PR19b · Socio inhabilitado: el carnet explica el motivo y ofrece cobrar. */
    @Test
    fun pr19b_socioInhabilitado_muestraMotivoYBotonCobrar() {
        PruebaUi.iniciarSesionAdmin()
        abrirCarnet(6, CarnetActivity.MODO_EMPLEADO).use {
            onView(withId(R.id.tvEstadoCarnet)).check(matches(withText("INHABILITADO")))
            onView(withId(R.id.tvMotivoEstado)).check(matches(withText(containsString("(14 día(s))"))))
            onView(withId(R.id.btnCobrarDesdeCarnet)).perform(scrollTo()).check(matches(isDisplayed()))

            onView(withId(R.id.btnCobrarDesdeCarnet)).perform(click())
            onView(withId(R.id.tvSocioNombre)).check(matches(withText("SOSA, Julio")))
        }
    }

    /** PR19c · Socio registrado sin la primera cuota: todavía no tiene carnet (vista del empleado). */
    @Test
    fun pr19c_socioSinPrimeraCuota_empleadoVeAvisoYBotonCobrar() {
        PruebaUi.iniciarSesionAdmin()
        abrirCarnet(9, CarnetActivity.MODO_EMPLEADO).use {
            onView(withId(R.id.llCarnet)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.tvSinCarnet)).check(matches(withText(containsString("RUBIO, Ana todavía no abonó"))))
            onView(withId(R.id.btnCobrarDesdeCarnet)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        }
    }

    /** PR19d · El mismo caso visto por el socio. */
    @Test
    fun pr19d_socioSinPrimeraCuota_socioVeCuandoSeEmite() {
        Sesion.nroSocio = 9
        abrirCarnet(9, CarnetActivity.MODO_SOCIO).use {
            onView(withId(R.id.tvBienvenidaSocio)).check(matches(withText("Hola, Ana")))
            onView(withId(R.id.tvSinCarnet))
                .check(matches(withText(containsString("Tu carnet se emite cuando abonás la primera cuota"))))
            onView(withId(R.id.btnCobrarDesdeCarnet)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }

    /** PR19e · Un socio no puede ver el carnet de otro socio (solo accede a su carnet). */
    @Test
    fun pr19e_socioNoPuedeVerElCarnetDeOtro() {
        Sesion.nroSocio = 1
        abrirCarnet(2, CarnetActivity.MODO_SOCIO).use { escenario ->
            assertEquals(Lifecycle.State.DESTROYED, escenario.state)
        }
    }

    /** PR19f · El empleado de consulta ve el carnet pero no las acciones de administrador. */
    @Test
    fun pr19f_rolConsulta_veElCarnetSinAcciones() {
        PruebaUi.iniciarSesionConsulta()
        abrirCarnet(8, CarnetActivity.MODO_EMPLEADO).use {
            onView(withId(R.id.tvEstadoCarnet)).check(matches(withText("PENDIENTE")))
            onView(withId(R.id.btnRegistrarApto)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }

    /** Cerrar sesión desde el carnet del socio. */
    @Test
    fun cerrarSesionDelSocio_cierraElCarnetYLaSesion() {
        Sesion.nroSocio = 1
        abrirCarnet(1, CarnetActivity.MODO_SOCIO).use { escenario ->
            onView(withId(R.id.btnCerrarSesion)).perform(click())
            Thread.sleep(250)
            escenario.moveToState(Lifecycle.State.DESTROYED)
            assertEquals(Lifecycle.State.DESTROYED, escenario.state)
            assertEquals(null, Sesion.nroSocio)
        }
    }
}
