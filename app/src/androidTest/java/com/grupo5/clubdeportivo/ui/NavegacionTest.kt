package com.grupo5.clubdeportivo.ui

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.grupo5.clubdeportivo.R
import com.grupo5.clubdeportivo.soporte.PruebaUi
import com.grupo5.clubdeportivo.util.Sesion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Navegación del Menú Principal, salida con confirmación y control automático de
 * vencimientos al abrir la app. Caso PR20 del plan de pruebas y casos PR20b a PR20e.
 */
@RunWith(AndroidJUnit4::class)
class NavegacionTest {

    @Before
    fun prepararBase() = PruebaUi.reiniciarBase()

    /** PR20 · SALIR con confirmación vuelve a la pantalla inicial y cierra la sesión. */
    @Test
    fun pr20_salirConConfirmacion_vuelveALaPantallaInicial() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.btnIngreso)).perform(click())
            onView(withId(R.id.etUsuario)).perform(replaceText("admin"))
            onView(withId(R.id.etClave)).perform(replaceText("admin123"), closeSoftKeyboard())
            onView(withId(R.id.btnIngresar)).perform(scrollTo(), click())

            onView(withId(R.id.btnSalir)).perform(click())
            onView(withText("¿Querés cerrar la sesión?")).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText("SALIR")).inRoot(isDialog()).perform(click())

            onView(withId(R.id.btnIngreso)).check(matches(isDisplayed()))
            assertNull(Sesion.empleado)
        }
    }

    /** PR20b · CANCELAR en la confirmación: sigue en el menú con la sesión abierta. */
    @Test
    fun pr20b_cancelarSalida_sigueEnElMenu() {
        PruebaUi.iniciarSesionAdmin()
        ActivityScenario.launch(MenuPrincipalActivity::class.java).use {
            onView(withId(R.id.btnSalir)).perform(click())
            onView(withText("CANCELAR")).inRoot(isDialog()).perform(click())
            onView(withId(R.id.tvSaludo)).check(matches(isDisplayed()))
            assertEquals("admin", Sesion.empleado?.usuario)
        }
    }

    /** PR20c · Control automático al abrir la app: inhabilita a quienes tienen la cuota vencida. */
    @Test
    fun pr20c_alAbrirLaApp_seInhabilitanLosSociosVencidos() {
        // Se simula que todos quedaron activos (por ejemplo, la app no se abrió durante varios días).
        PruebaUi.conBase { bd -> bd.writableDatabase.execSQL("UPDATE socio SET estado_activo = 1") }
        assertEquals(9, PruebaUi.contar("SELECT COUNT(*) FROM socio WHERE estado_activo = 1"))

        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.btnIngreso)).check(matches(isDisplayed()))
            // Quedan activos solo GOMEZ, RUIZ, PEREZ y ACOSTA (cuota al día o que vence hoy).
            assertEquals(4, PruebaUi.contar("SELECT COUNT(*) FROM socio WHERE estado_activo = 1"))
            assertEquals(0, PruebaUi.contar(
                "SELECT COUNT(*) FROM socio WHERE estado_activo = 1 AND nro_socio IN (4, 5, 6, 7, 9)"))
        }
    }

    /** PR20d · Los botones del Menú Principal abren cada módulo. */
    @Test
    fun pr20d_botonesDelMenu_abrenCadaPantalla() {
        PruebaUi.iniciarSesionAdmin()
        ActivityScenario.launch(MenuPrincipalActivity::class.java).use {
            onView(withId(R.id.btnRegistroSocio)).perform(scrollTo(), click())
            onView(withId(R.id.tvTituloBarra)).check(matches(withText("ALTA DE SOCIO")))
            onView(withId(R.id.btnVolver)).perform(click())

            onView(withId(R.id.btnCobroCuota)).perform(scrollTo(), click())
            onView(withId(R.id.tvTituloBarra)).check(matches(withText("COBRAR CUOTA")))
            onView(withId(R.id.btnVolver)).perform(click())

            onView(withId(R.id.btnVerVencimientos)).perform(scrollTo(), click())
            onView(withId(R.id.tvTituloBarra)).check(matches(withText("VENCIMIENTOS")))
            onView(withId(R.id.btnVolver)).perform(click())

            // La tarjeta de "vencen hoy" también abre el listado.
            onView(withId(R.id.cardVencimientos)).perform(scrollTo(), click())
            onView(withId(R.id.tvTituloBarra)).check(matches(withText("VENCIMIENTOS")))
        }
    }

    /** PR20e · Sin sesión (por ejemplo, Android cerró el proceso), el menú vuelve al inicio. */
    @Test
    fun pr20e_menuSinSesion_vuelveALaPantallaInicial() {
        ActivityScenario.launch(MenuPrincipalActivity::class.java).use { escenario ->
            assertEquals(Lifecycle.State.DESTROYED, escenario.state)
            onView(withId(R.id.btnIngreso)).check(matches(isDisplayed()))
        }
    }
}
