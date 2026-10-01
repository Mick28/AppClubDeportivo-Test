package com.grupo5.clubdeportivo.soporte

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.grupo5.clubdeportivo.data.BaseDatosClub
import com.grupo5.clubdeportivo.util.Sesion
import org.hamcrest.Description
import org.hamcrest.Matcher

/**
 * Utilidades compartidas por todas las pruebas de instrumentación.
 *
 * Cada prueba arranca con la base recién creada (los mismos usuarios y socios de prueba
 * de la sección 11 del documento) y sin sesión iniciada, para que los resultados no
 * dependan del orden en que se ejecutan las pruebas.
 *
 * IMPORTANTE: las pruebas borran la base clubdeportivo.db de la app instalada en el
 * emulador. No correrlas en un celular con datos reales.
 */
object PruebaUi {

    val contexto: Context
        get() = ApplicationProvider.getApplicationContext()

    /** Cierra la sesión y borra la base: al abrirse de nuevo se vuelven a cargar los datos de prueba. */
    fun reiniciarBase() {
        cerrarTodasLasActividades()
        Sesion.cerrar()
        contexto.deleteDatabase(BaseDatosClub.NOMBRE_BD)
    }

    /** Inicia sesión como empleado sin pasar por la pantalla de login (para probar una pantalla aislada). */
    fun iniciarSesionComo(usuario: String, clave: String) {
        conBase { bd ->
            Sesion.empleado = requireNotNull(bd.validarEmpleado(usuario, clave)) {
                "El usuario de prueba $usuario no existe en DatosDePrueba"
            }
        }
    }

    fun iniciarSesionAdmin() = iniciarSesionComo("admin", "admin123")

    fun iniciarSesionConsulta() = iniciarSesionComo("recepcion", "recep123")

    /** Abre la base, ejecuta el bloque y la cierra. */
    fun <T> conBase(bloque: (BaseDatosClub) -> T): T {
        val bd = BaseDatosClub(contexto)
        try {
            return bloque(bd)
        } finally {
            bd.close()
        }
    }

    /** Ejecuta una consulta SELECT COUNT(*) directamente sobre la base (para verificar el resultado de una acción). */
    fun contar(sql: String, vararg args: String): Int = conBase { bd ->
        bd.readableDatabase.rawQuery(sql, args).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
    }

    /**
     * Cierra las pantallas que hayan quedado abiertas por una prueba anterior (por ejemplo, el
     * recibo o el carnet que se abren desde otra Activity), así cada prueba empieza de cero.
     */
    fun cerrarTodasLasActividades() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            listOf(Stage.PRE_ON_CREATE, Stage.CREATED, Stage.STARTED, Stage.RESUMED, Stage.PAUSED, Stage.STOPPED)
                .flatMap { monitor.getActivitiesInStage(it) }
                .forEach { it.finish() }
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }
}

// -------------------------------------------------------------------------------------------
// Matchers y acciones para el RecyclerView del listado de vencimientos
// -------------------------------------------------------------------------------------------

/** Verifica que la fila de la posición indicada cumpla con el matcher (por ejemplo, que tenga un texto). */
fun enPosicion(posicion: Int, matcherFila: Matcher<View>): Matcher<View> =
    object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("fila $posicion: ")
            matcherFila.describeTo(description)
        }

        override fun matchesSafely(rv: RecyclerView): Boolean {
            val holder = rv.findViewHolderForAdapterPosition(posicion) ?: return false
            return matcherFila.matches(holder.itemView)
        }
    }

/** Verifica la cantidad de filas del adaptador. */
fun conCantidadDeFilas(cantidad: Int): Matcher<View> =
    object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("RecyclerView con $cantidad filas")
        }

        override fun matchesSafely(rv: RecyclerView): Boolean = rv.adapter?.itemCount == cantidad
    }

/** Toca la fila completa (itemView), sin riesgo de caer sobre el botón COBRAR. */
fun tocarFila(): ViewAction = object : ViewAction {
    override fun getConstraints(): Matcher<View> = isDisplayed()
    override fun getDescription() = "tocar la fila"
    override fun perform(uiController: UiController, view: View) {
        view.performClick()
        uiController.loopMainThreadUntilIdle()
    }
}

/** Toca una vista hija de la fila (por ejemplo, el botón COBRAR). */
fun tocarHijo(idVista: Int): ViewAction = object : ViewAction {
    override fun getConstraints(): Matcher<View> = isDisplayed()
    override fun getDescription() = "tocar la vista hija $idVista de la fila"
    override fun perform(uiController: UiController, view: View) {
        view.findViewById<View>(idVista).performClick()
        uiController.loopMainThreadUntilIdle()
    }
}
