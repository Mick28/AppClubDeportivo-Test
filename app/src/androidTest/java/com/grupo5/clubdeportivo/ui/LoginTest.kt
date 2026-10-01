package com.grupo5.clubdeportivo.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.grupo5.clubdeportivo.R
import com.grupo5.clubdeportivo.soporte.PruebaUi
import com.grupo5.clubdeportivo.util.Sesion
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Módulo 1 · Pantalla inicial e inicio de sesión con perfil. Casos PR01 a PR04 del plan
 * de pruebas (sección 12) y casos complementarios PR01b a PR03c.
 */
@RunWith(AndroidJUnit4::class)
class LoginTest {

    @Before
    fun prepararBase() = PruebaUi.reiniciarBase()

    private fun ingresar(usuario: String, clave: String, comoSocio: Boolean = false) {
        if (comoSocio) onView(withId(R.id.rbSocio)).perform(click())
        onView(withId(R.id.etUsuario)).perform(replaceText(usuario))
        onView(withId(R.id.etClave)).perform(replaceText(clave), closeSoftKeyboard())
        onView(withId(R.id.btnIngresar)).perform(scrollTo(), click())
    }

    /** PR01 · Ingreso de empleado desde la pantalla inicial. */
    @Test
    fun pr01_ingresoDeEmpleado_abreElMenuPrincipal() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.btnIngreso)).check(matches(withText("INGRESO A LA APP"))).perform(click())
            ingresar("admin", "admin123")

            onView(withId(R.id.tvSaludo)).check(matches(isDisplayed()))
            onView(withId(R.id.tvRol)).check(matches(withText("ADMIN")))
            onView(withId(R.id.tvCantVencenHoy)).check(matches(withText("2")))
            onView(withId(R.id.tvTextoVencenHoy)).check(matches(withText("socios con cuota que vence hoy")))
            assertEquals("admin", Sesion.empleado?.usuario)
        }
    }

    /** PR02 · Ingreso de socio: solo ve su carnet. */
    @Test
    fun pr02_ingresoDeSocio_abreSoloSuCarnet() {
        ActivityScenario.launch(LoginActivity::class.java).use {
            ingresar("agomez", "socio123", comoSocio = true)

            onView(withId(R.id.tvApellidoCarnet)).check(matches(withText("GOMEZ")))
            onView(withId(R.id.tvNombreCarnet)).check(matches(withText("Ana")))
            onView(withId(R.id.tvEstadoCarnet)).check(matches(withText("HABILITADO")))
            onView(withId(R.id.tvBienvenidaSocio)).check(matches(withText("Hola, Ana")))
            onView(withId(R.id.btnCerrarSesion)).check(matches(isDisplayed()))
            // El socio no tiene botón de volver ni acciones de empleado.
            onView(withId(R.id.btnVolver)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onView(withId(R.id.btnRegistrarApto)).check(matches(withEffectiveVisibility(Visibility.GONE)))
            assertNull(Sesion.empleado)
            assertEquals(1, Sesion.nroSocio)
        }
    }

    /** PR02b · El alias del socio no distingue mayúsculas (se pasa a minúsculas). */
    @Test
    fun pr02b_aliasEnMayusculas_tambienIngresa() {
        ActivityScenario.launch(LoginActivity::class.java).use {
            ingresar("AGOMEZ", "socio123", comoSocio = true)
            onView(withId(R.id.tvApellidoCarnet)).check(matches(withText("GOMEZ")))
        }
    }

    /** PR03 · Credenciales incorrectas: mensaje genérico, sin avanzar (HU01 escenario 2). */
    @Test
    fun pr03_credencialesIncorrectas_muestraErrorSinAvanzar() {
        ActivityScenario.launch(LoginActivity::class.java).use {
            ingresar("admin", "clave-erronea")

            onView(withId(R.id.tvError)).check(matches(withText("Usuario o contraseña incorrectos.")))
            onView(withId(R.id.btnIngresar)).check(matches(isDisplayed()))
            assertNull(Sesion.empleado)
        }
    }

    /** PR03b · Socio con clave incorrecta. */
    @Test
    fun pr03b_socioConClaveIncorrecta_muestraError() {
        ActivityScenario.launch(LoginActivity::class.java).use {
            ingresar("agomez", "otra", comoSocio = true)
            onView(withId(R.id.tvError)).check(matches(withText("Alias o contraseña incorrectos.")))
        }
    }

    /** PR03c · Campos vacíos: pide el dato que falta y cambia la etiqueta según el perfil. */
    @Test
    fun pr03c_camposVacios_pideElDatoQueFalta() {
        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(withId(R.id.btnIngresar)).perform(scrollTo(), click())
            onView(withId(R.id.tvError)).check(matches(withText("Ingresá tu usuario.")))

            onView(withId(R.id.rbSocio)).perform(click())
            onView(withId(R.id.tvEtiquetaUsuario)).check(matches(withText("Alias de socio")))
            onView(withId(R.id.etUsuario)).perform(replaceText("agomez"), closeSoftKeyboard())
            onView(withId(R.id.btnIngresar)).perform(scrollTo(), click())
            onView(withId(R.id.tvError)).check(matches(withText("Ingresá tu contraseña.")))
        }
    }

    /** PR04 · Rol de consulta: registro y cobro deshabilitados, con aviso. */
    @Test
    fun pr04_rolConsulta_noPuedeRegistrarNiCobrar() {
        ActivityScenario.launch(LoginActivity::class.java).use {
            ingresar("recepcion", "recep123")

            onView(withId(R.id.tvRol)).check(matches(withText("CONSULTA")))
            onView(withId(R.id.btnRegistroSocio)).check(matches(not(isEnabled())))
            onView(withId(R.id.btnCobroCuota)).check(matches(not(isEnabled())))
            onView(withId(R.id.btnVerVencimientos)).check(matches(isEnabled()))
            onView(withId(R.id.tvAvisoRol)).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }
}
