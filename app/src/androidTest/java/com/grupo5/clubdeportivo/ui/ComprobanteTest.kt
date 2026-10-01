package com.grupo5.clubdeportivo.ui

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.intent.matcher.IntentMatchers.isInternal
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.grupo5.clubdeportivo.R
import com.grupo5.clubdeportivo.soporte.PruebaUi
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Módulo 2 · Documento de pago. Verifica que el recibo muestre los siete datos que pide el
 * libro (Tema II 8.4) recibidos con putExtra()/getStringExtra(), y que el botón Compartir
 * use un Intent implícito (ACTION_SEND). Casos PR10b y PR10c.
 */
@RunWith(AndroidJUnit4::class)
class ComprobanteTest {

    @Before
    fun preparar() {
        PruebaUi.reiniciarBase()
        PruebaUi.iniciarSesionAdmin()
        Intents.init()
    }

    @After
    fun liberarIntents() = Intents.release()

    private fun intentRecibo(estado: String = "HABILITADO", carnetEmitido: Boolean = false) =
        Intent(PruebaUi.contexto, ComprobanteActivity::class.java).apply {
            putExtra(ComprobanteActivity.NRO_DOCUMENTO, "000012")
            putExtra(ComprobanteActivity.NOMBRE_DOCUMENTO, "RECIBO DE PAGO")
            putExtra(ComprobanteActivity.FECHA, "30/09/2026")
            putExtra(ComprobanteActivity.EMISOR, "Club Deportivo · CUIT 30-71234567-8\nAtendió: GONZALEZ, Emma")
            putExtra(ComprobanteActivity.RECEPTOR, "SOSA, Julio\nSocio N° 6 · DNI 29555666")
            putExtra(ComprobanteActivity.DETALLE, "Cuota social mensual\nForma de pago: Efectivo")
            putExtra(ComprobanteActivity.MONTO, "$ 18.000")
            putExtra(ComprobanteActivity.VENCIMIENTO, "30/10/2026")
            putExtra(ComprobanteActivity.ESTADO, estado)
            putExtra(ComprobanteActivity.NRO_SOCIO, "6")
            putExtra(ComprobanteActivity.CARNET_EMITIDO, carnetEmitido.toString())
        }

    /** PR10b · El recibo muestra número, nombre, fecha, emisor, receptor, detalle y monto. */
    @Test
    fun pr10b_reciboMuestraLosSieteDatos() {
        ActivityScenario.launch<ComprobanteActivity>(intentRecibo()).use {
            onView(withId(R.id.tvNroDocumento)).check(matches(withText("N° 000012")))
            onView(withId(R.id.tvNombreDocumento)).check(matches(withText("RECIBO DE PAGO")))
            onView(withId(R.id.tvFechaDocumento)).check(matches(withText("Fecha: 30/09/2026")))
            onView(withId(R.id.tvEmisor)).check(matches(withText(containsString("CUIT 30-71234567-8"))))
            onView(withId(R.id.tvReceptor)).check(matches(withText(containsString("Socio N° 6 · DNI 29555666"))))
            onView(withId(R.id.tvDetalle)).check(matches(withText(containsString("Cuota social mensual"))))
            onView(withId(R.id.tvMonto)).check(matches(withText("$ 18.000")))
            onView(withId(R.id.tvEstadoResultante))
                .check(matches(withText(containsString("HABILITADO para realizar actividades hasta el 30/10/2026"))))
            onView(withId(R.id.tvEstadoResultante)).check(matches(not(withText(containsString("Se emitió el carnet")))))
        }
    }

    /** PR10b · Si el socio no tiene apto físico, el recibo avisa que queda PENDIENTE. */
    @Test
    fun pr10b_socioSinApto_quedaPendiente() {
        ActivityScenario.launch<ComprobanteActivity>(intentRecibo(estado = "PENDIENTE", carnetEmitido = true)).use {
            onView(withId(R.id.tvEstadoResultante)).check(matches(withText(containsString("PENDIENTE"))))
            onView(withId(R.id.tvEstadoResultante)).check(matches(withText(containsString("Se emitió el carnet"))))
        }
    }

    /** PR10c · Compartir: Intent implícito ACTION_SEND con el texto del recibo. */
    @Test
    fun pr10c_compartir_usaIntentImplicitoActionSend() {
        // Se responde en lugar del selector del sistema para que la prueba no salga de la app.
        intending(not(isInternal())).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

        ActivityScenario.launch<ComprobanteActivity>(intentRecibo()).use {
            onView(withId(R.id.btnCompartir)).perform(scrollTo(), click())

            intended(allOf(
                hasAction(Intent.ACTION_CHOOSER),
                hasExtra(equalTo(Intent.EXTRA_INTENT), allOf(
                    hasAction(Intent.ACTION_SEND),
                    hasType("text/plain"),
                    hasExtra(equalTo(Intent.EXTRA_TEXT), containsString("TOTAL: $ 18.000"))
                ))
            ))
        }
    }

    /** VER CARNET abre el carnet del socio del recibo. */
    @Test
    fun verCarnet_abreElCarnetDelSocio() {
        ActivityScenario.launch<ComprobanteActivity>(intentRecibo()).use {
            onView(withId(R.id.btnVerCarnet)).perform(scrollTo(), click())
            onView(withId(R.id.tvApellidoCarnet)).check(matches(withText("SOSA")))
        }
    }
}
