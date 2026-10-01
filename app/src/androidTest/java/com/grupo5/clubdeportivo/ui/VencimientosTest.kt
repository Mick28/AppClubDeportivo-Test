package com.grupo5.clubdeportivo.ui

import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.grupo5.clubdeportivo.R
import com.grupo5.clubdeportivo.soporte.PruebaUi
import com.grupo5.clubdeportivo.soporte.conCantidadDeFilas
import com.grupo5.clubdeportivo.soporte.enPosicion
import com.grupo5.clubdeportivo.soporte.tocarFila
import com.grupo5.clubdeportivo.soporte.tocarHijo
import com.grupo5.clubdeportivo.util.Formato
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Módulo 3 · Listado diario de vencimientos con RecyclerView. Casos PR16, PR17 y PR18 del
 * plan de pruebas y casos complementarios PR16b a PR18b.
 */
@RunWith(AndroidJUnit4::class)
class VencimientosTest {

    @Before
    fun prepararBase() = PruebaUi.reiniciarBase()

    private fun fila(posicion: Int, texto: String) =
        onView(withId(R.id.rvSocios)).check(matches(enPosicion(posicion, hasDescendant(withText(texto)))))

    /** PR16 · Listado del día: título, fecha y 4 socios ordenados por apellido. */
    @Test
    fun pr16_listadoDelDia_ordenadoPorApellidoYNombre() {
        PruebaUi.iniciarSesionAdmin()
        ActivityScenario.launch(VencimientosActivity::class.java).use {
            onView(withId(R.id.tvTituloBarra)).check(matches(withText("VENCIMIENTOS")))
            onView(withId(R.id.tvFechaHoy)).check(matches(withText(Formato.fechaLarga(LocalDate.now()).uppercase())))
            onView(withId(R.id.tvCantHoy)).check(matches(withText("4")))
            onView(withId(R.id.tvCantAcumuladas)).check(matches(withText("2")))

            onView(withId(R.id.rvSocios)).check(matches(conCantidadDeFilas(4)))
            fila(0, "DIAZ, Carla"); fila(0, "VENCIÓ AYER")
            fila(1, "LOPEZ, Hugo"); fila(1, "VENCIÓ AYER")
            fila(2, "PEREZ, Luis"); fila(2, "VENCE HOY")
            fila(3, "RUIZ, Marta"); fila(3, "VENCE HOY")
            fila(0, "Socio N° 4")
            fila(0, "11 3333-2222")
        }
    }

    /** PR17 · Solapa de vencidas acumuladas: del más atrasado al más reciente. */
    @Test
    fun pr17_vencidasAcumuladas() {
        PruebaUi.iniciarSesionAdmin()
        ActivityScenario.launch(VencimientosActivity::class.java).use {
            onView(withId(R.id.tabAcumuladas)).perform(click())

            onView(withId(R.id.rvSocios)).check(matches(conCantidadDeFilas(2)))
            fila(0, "BENITEZ, Rosa"); fila(0, "40 DÍAS")
            fila(1, "SOSA, Julio"); fila(1, "14 DÍAS")

            onView(withId(R.id.tabHoy)).perform(click())
            onView(withId(R.id.rvSocios)).check(matches(conCantidadDeFilas(4)))
        }
    }

    /** PR18 · Tocar un socio abre su carnet; COBRAR abre el cobro y, al volver, el socio ya no figura. */
    @Test
    fun pr18_interaccionDelListado_carnetYCobro() {
        PruebaUi.iniciarSesionAdmin()
        ActivityScenario.launch(VencimientosActivity::class.java).use {
            // Tocar DIAZ → carnet.
            onView(withId(R.id.rvSocios)).perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, tocarFila()))
            onView(withId(R.id.tvApellidoCarnet)).check(matches(withText("DIAZ")))
            onView(withId(R.id.tvEstadoCarnet)).check(matches(withText("INHABILITADO")))
            pressBack()

            // COBRAR en LOPEZ → cobro con el socio cargado → pagar.
            onView(withId(R.id.rvSocios))
                .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(1, tocarHijo(R.id.btnCobrar)))
            onView(withId(R.id.tvSocioNombre)).check(matches(withText("LOPEZ, Hugo")))
            onView(withId(R.id.btnRegistrarPago)).perform(scrollTo(), click())
            onView(withText("REGISTRAR")).inRoot(isDialog()).perform(click())
            onView(withId(R.id.tvNombreDocumento)).check(matches(withText("RECIBO DE PAGO")))

            // Volver desde el recibo: el listado se vuelve a consultar en onResume().
            pressBack()
            onView(withId(R.id.tvCantHoy)).check(matches(withText("3")))
            onView(withId(R.id.rvSocios)).check(matches(conCantidadDeFilas(3)))
            onView(withText("LOPEZ, Hugo")).check(doesNotExist())
        }
    }

    /** PR18b · Con rol CONSULTA el botón COBRAR no aparece en las filas. */
    @Test
    fun pr18b_rolConsulta_noVeElBotonCobrar() {
        PruebaUi.iniciarSesionConsulta()
        ActivityScenario.launch(VencimientosActivity::class.java).use {
            onView(withId(R.id.rvSocios)).check(matches(enPosicion(0,
                hasDescendant(allOf(withId(R.id.btnCobrar), withEffectiveVisibility(Visibility.GONE))))))
        }
    }

    /** PR16b · Estado vacío: si nadie vence, se muestra el aviso en lugar de la lista. */
    @Test
    fun pr16b_sinVencimientos_muestraEstadoVacio() {
        PruebaUi.conBase { bd -> bd.writableDatabase.execSQL("UPDATE socio SET baja = 1") }
        PruebaUi.iniciarSesionAdmin()
        ActivityScenario.launch(VencimientosActivity::class.java).use {
            onView(withId(R.id.tvCantHoy)).check(matches(withText("0")))
            onView(withId(R.id.tvVacio)).check(matches(isDisplayed()))
            onView(withId(R.id.rvSocios)).check(matches(withEffectiveVisibility(Visibility.INVISIBLE)))
        }
    }
}
