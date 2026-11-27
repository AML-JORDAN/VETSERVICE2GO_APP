package com.example.appvetservice

// Una cita tal como está guardada en Firebase (colección "citas")
data class CitaItem(
    val id: Long,                // el folio: 1, 2, 3...
    val usuarioId: String,       // de qué cuenta es
    val cliente: String,         // nombre de esa cuenta
    val propietario: String,
    val mascota: String,
    val especie: String,
    val raza: String,
    val peso: Double,
    val foto: String,            // "data:image/jpeg;base64,..." o "" si no tiene
    val servicios: String,       // "Consulta General, Vacunación"
    val total: Int,
    val fechaCita: String,       // "2026-11-27"
    val horario: String,         // "15:30 - 16:10"
    val estado: String,          // "pagado", "completado" o "rechazado"
    val atendidoPor: String,     // nombre del empleado que la completó o rechazó
    val motivoRechazo: String,
    val completadoEn: String
)
