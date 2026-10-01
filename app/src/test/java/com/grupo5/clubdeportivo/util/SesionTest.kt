package com.grupo5.clubdeportivo.util

import com.grupo5.clubdeportivo.model.Empleado
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Test

/** Prueba unitaria local de la sesión en memoria (Sesion). Grupo PU-05. */
class SesionTest {

    @After
    fun limpiarSesion() = Sesion.cerrar()

    @Test
    fun cerrar_borraEmpleadoYSocio() {
        Sesion.empleado = Empleado(1, "admin", "GONZALEZ", "Emma", Empleado.ROL_ADMIN)
        Sesion.nroSocio = 1
        Sesion.cerrar()
        assertNull(Sesion.empleado)
        assertNull(Sesion.nroSocio)
    }
}
