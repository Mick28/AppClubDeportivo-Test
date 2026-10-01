package com.grupo5.clubdeportivo.ui

import android.widget.DatePicker
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.matcher.ViewMatchers.hasErrorText
import androidx.test.espresso.matcher.ViewMatchers.hasFocus
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.grupo5.clubdeportivo.R
import com.grupo5.clubdeportivo.soporte.PruebaUi
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Módulo 1 · Registro de socio: validaciones antes de insertar. Casos PR05 a PR08 del plan
 * de pruebas y casos complementarios de formato (PR08b a PR08f).
 *
 * El alta completa (PR09 en adelante) está en [FlujoAltaSocioTest].
 */
@RunWith(AndroidJUnit4::class)
class RegistroSocioTest {

    @Before
    fun prepararBaseYSesion() {
        PruebaUi.reiniciarBase()
        PruebaUi.iniciarSesionAdmin()
    }

    /** Datos válidos de un socio nuevo; cada prueba cambia solo el dato que quiere probar. */
    data class Formulario(
        val dni: String = "40123456",
        val apellido: String = "Prueba",
        val nombre: String = "Tomás",
        val telefono: String = "11 1234-5678",
        val email: String = "tprueba@mail.com",
        val alias: String = "tprueba",
        val clave: String = "clave123",
        val claveRepetida: String = "clave123",
        val aptoFisico: Boolean = true,
        val elegirFecha: Boolean = true
    )

    private fun completar(f: Formulario) {
        onView(withId(R.id.etNroDoc)).perform(scrollTo(), replaceText(f.dni))
        onView(withId(R.id.etApellido)).perform(scrollTo(), replaceText(f.apellido))
        onView(withId(R.id.etNombre)).perform(scrollTo(), replaceText(f.nombre), closeSoftKeyboard())
        if (f.elegirFecha) elegirFechaDeNacimiento(2000, 1, 15)
        onView(withId(R.id.etTelefono)).perform(scrollTo(), replaceText(f.telefono))
        onView(withId(R.id.etEmail)).perform(scrollTo(), replaceText(f.email))
        if (f.aptoFisico) onView(withId(R.id.cbAptoFisico)).perform(scrollTo(), click())
        onView(withId(R.id.etAlias)).perform(scrollTo(), replaceText(f.alias))
        onView(withId(R.id.etClave)).perform(scrollTo(), replaceText(f.clave))
        onView(withId(R.id.etClaveRepetida)).perform(scrollTo(), replaceText(f.claveRepetida), closeSoftKeyboard())
    }

    private fun guardar() = onView(withId(R.id.btnGuardar)).perform(scrollTo(), click())

    private fun socios() = PruebaUi.contar("SELECT COUNT(*) FROM socio")

    /** PR05 · Guardar el formulario vacío: marca los obligatorios y pone el foco en el primero. */
    @Test
    fun pr05_camposObligatoriosVacios_seMarcanYNoSeGuarda() {
        ActivityScenario.launch(RegistroSocioActivity::class.java).use {
            guardar()

            onView(withId(R.id.etNroDoc)).check(matches(hasErrorText("Dato obligatorio")))
            onView(withId(R.id.etApellido)).check(matches(hasErrorText("Dato obligatorio")))
            onView(withId(R.id.etAlias)).check(matches(hasErrorText("Dato obligatorio")))
            onView(withId(R.id.etFechaNac)).check(matches(hasErrorText("Elegí la fecha de nacimiento")))
            onView(withId(R.id.etNroDoc)).check(matches(hasFocus()))
            assertEquals(9, socios())
        }
    }

    /** PR06 · Alias repetido: aviso, foco en el alias y no se guarda. */
    @Test
    fun pr06_aliasRepetido_marcaElAliasYNoSeGuarda() {
        ActivityScenario.launch(RegistroSocioActivity::class.java).use {
            completar(Formulario(alias = "agomez"))
            guardar()

            onView(withId(R.id.etAlias)).check(matches(hasErrorText("Alias en uso")))
            onView(withId(R.id.etAlias)).check(matches(hasFocus()))
            assertEquals(9, socios())
        }
    }

    /** PR06b · El alias se compara en minúsculas: "AGomez" también está en uso. */
    @Test
    fun pr06b_aliasRepetidoConMayusculas_tambienSeDetecta() {
        ActivityScenario.launch(RegistroSocioActivity::class.java).use {
            completar(Formulario(alias = "AGomez"))
            guardar()
            onView(withId(R.id.etAlias)).check(matches(hasErrorText("Alias en uso")))
        }
    }

    /** PR07 · Documento ya registrado como socio (GOMEZ, Ana). */
    @Test
    fun pr07_documentoRepetido_marcaElDocumento() {
        ActivityScenario.launch(RegistroSocioActivity::class.java).use {
            completar(Formulario(dni = "30111222"))
            guardar()

            onView(withId(R.id.etNroDoc)).check(matches(hasErrorText("Documento ya registrado")))
            assertEquals(9, socios())
        }
    }

    /** PR08 · DNI con menos de 7 dígitos. */
    @Test
    fun pr08_dniConFormatoInvalido_marcaElDocumento() {
        ActivityScenario.launch(RegistroSocioActivity::class.java).use {
            completar(Formulario(dni = "12345"))
            guardar()
            onView(withId(R.id.etNroDoc)).check(matches(hasErrorText("El DNI debe tener 7 u 8 dígitos, sin puntos")))
        }
    }

    /** PR08b · Correo sin @. */
    @Test
    fun pr08b_emailSinArroba_marcaElCorreo() {
        ActivityScenario.launch(RegistroSocioActivity::class.java).use {
            completar(Formulario(email = "correo.sin.arroba"))
            guardar()
            onView(withId(R.id.etEmail)).check(matches(hasErrorText("Ingresá un correo válido")))
        }
    }

    /** PR08c · Contraseñas distintas. */
    @Test
    fun pr08c_contraseniasDistintas_marcaLaRepeticion() {
        ActivityScenario.launch(RegistroSocioActivity::class.java).use {
            completar(Formulario(claveRepetida = "otra-clave"))
            guardar()
            onView(withId(R.id.etClaveRepetida)).check(matches(hasErrorText("Las contraseñas no coinciden")))
        }
    }

    /** PR08d · Contraseña de menos de 6 caracteres. */
    @Test
    fun pr08d_contraseniaCorta_marcaLaContrasenia() {
        ActivityScenario.launch(RegistroSocioActivity::class.java).use {
            completar(Formulario(clave = "123", claveRepetida = "123"))
            guardar()
            onView(withId(R.id.etClave)).check(matches(hasErrorText("La contraseña debe tener al menos 6 caracteres")))
        }
    }

    /** PR08e · Alias con espacios. */
    @Test
    fun pr08e_aliasConEspacios_marcaElAlias() {
        ActivityScenario.launch(RegistroSocioActivity::class.java).use {
            completar(Formulario(alias = "tomas prueba"))
            guardar()
            onView(withId(R.id.etAlias))
                .check(matches(hasErrorText("Entre 4 y 20 letras, números, punto o guion bajo, sin espacios")))
        }
    }

    /** PR08f · Teléfono con letras. */
    @Test
    fun pr08f_telefonoConLetras_marcaElTelefono() {
        ActivityScenario.launch(RegistroSocioActivity::class.java).use {
            completar(Formulario(telefono = "llamar al club"))
            guardar()
            onView(withId(R.id.etTelefono)).check(matches(hasErrorText("Solo números, espacios, guiones o paréntesis")))
        }
    }

    /** Control de acceso (M06): un empleado de consulta no puede abrir el registro. */
    @Test
    fun rolConsulta_noPuedeAbrirElRegistro() {
        PruebaUi.iniciarSesionConsulta()
        ActivityScenario.launch(RegistroSocioActivity::class.java).use { escenario ->
            assertEquals(Lifecycle.State.DESTROYED, escenario.state)
        }
    }

    /** CANCELAR cierra la pantalla sin guardar. */
    @Test
    fun cancelar_cierraSinGuardar() {
        ActivityScenario.launch(RegistroSocioActivity::class.java).use { escenario ->
            completar(Formulario())
            onView(withId(R.id.btnCancelar)).perform(scrollTo(), click())
            assertEquals(Lifecycle.State.DESTROYED, escenario.state)
            assertEquals(9, socios())
        }
    }

    companion object {
        /** Elige la fecha en el DatePickerDialog (mes de 1 a 12) y confirma con OK. */
        fun elegirFechaDeNacimiento(anio: Int, mes: Int, dia: Int) {
            onView(withId(R.id.etFechaNac)).perform(scrollTo(), click())
            onView(withClassName(equalTo(DatePicker::class.java.name)))
                .perform(PickerActions.setDate(anio, mes, dia))
            onView(withId(android.R.id.button1)).perform(click())
            onView(withId(R.id.etFechaNac)).check(matches(isDisplayed()))
        }
    }
}
