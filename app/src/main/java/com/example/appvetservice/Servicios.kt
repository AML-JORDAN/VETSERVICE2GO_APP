package com.example.appvetservice

// Servicios de la veterinaria y su precio, en UN solo lugar.
// Si cambia un precio, se cambia aquí y se actualiza en toda la app.
object Servicios {

    const val CONSULTA = "Consulta General"
    const val VACUNACION = "Vacunación"
    const val DESPARASITACION = "Desparasitación"
    const val BANO = "Baño y Estética"
    const val URGENCIAS = "Urgencias"

    private val precios = mapOf(
        CONSULTA to 350,
        VACUNACION to 400,
        DESPARASITACION to 250,
        BANO to 300,
        URGENCIAS to 600
    )

    fun precio(nombre: String): Int = precios.getValue(nombre)

    // "Consulta General" -> "Consulta General ($350)"
    fun conPrecio(nombre: String): String = "$nombre (\$${precio(nombre)})"
}
