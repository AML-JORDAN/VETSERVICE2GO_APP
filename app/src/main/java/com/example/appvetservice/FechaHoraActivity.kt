package com.example.appvetservice

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Button
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import java.util.Calendar
import java.util.Locale

// PANTALLA: FECHA Y HORA
// Sirve para agendar una cita nueva y también para REAGENDAR una cita rechazada
// (cuando llega el dato "REAGENDAR_ID" desde Mis Citas).
class FechaHoraActivity : AppCompatActivity() {

    // Los 4 horarios de atención, en el mismo orden que los botones (los mismos de la página web)
    private val horarios = listOf("15:30 - 16:10", "16:00 - 16:40", "16:30 - 17:10", "17:00 - 17:40")

    private lateinit var botonesHorarios: List<Button>

    private var fechaSeleccionada: String = ""      // "2026-11-27", como se guarda en Firebase
    private var horarioSeleccionado: String = ""
    private var horariosOcupados: List<String> = emptyList()

    // -1 = cita nueva. Otro número = el folio de la cita rechazada que se va a reagendar.
    private var reagendarId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_fecha_hora)

        reagendarId = intent.getLongExtra("REAGENDAR_ID", -1)

        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val btnConfirmar = findViewById<Button>(R.id.btnConfirmarCita)
        botonesHorarios = listOf(
            findViewById(R.id.btnHorario1),
            findViewById(R.id.btnHorario2),
            findViewById(R.id.btnHorario3),
            findViewById(R.id.btnHorario4)
        )

        // Al reagendar cambian los títulos y el botón
        if (reagendarId != -1L) {
            val mascota = intent.getStringExtra("MASCOTA") ?: ""
            findViewById<TextView>(R.id.tvPasoFecha).setText(R.string.reagendar_paso)
            findViewById<TextView>(R.id.tvTituloFecha).text = getString(R.string.reagendar_titulo, mascota)
            btnConfirmar.setText(R.string.reagendar_confirmar)
        }

        // Las citas se agendan a partir de mañana (como en la página web):
        // minDate no deja tocar los días anteriores en el calendario
        val manana = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        calendarView.minDate = manana.timeInMillis
        calendarView.date = manana.timeInMillis
        fechaSeleccionada = formatearFecha(
            manana.get(Calendar.DAY_OF_MONTH),
            manana.get(Calendar.MONTH) + 1,
            manana.get(Calendar.YEAR)
        )

        // Al cambiar de día hay que volver a elegir horario y revisar cuáles están ocupados
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            fechaSeleccionada = formatearFecha(dayOfMonth, month + 1, year)
            horarioSeleccionado = ""
            actualizarHorarios()
            Toast.makeText(this, "Fecha: ${BaseDeDatos.mostrarFecha(fechaSeleccionada)}", Toast.LENGTH_SHORT).show()
        }

        // Cada botón elige su horario (el botón 1 es el horario 1, y así)
        for ((i, boton) in botonesHorarios.withIndex()) {
            boton.setOnClickListener {
                horarioSeleccionado = horarios[i]
                pintarHorarios()
                Toast.makeText(this, "Horario seleccionado: $horarioSeleccionado", Toast.LENGTH_SHORT).show()
            }
        }

        btnConfirmar.setOnClickListener {
            if (horarioSeleccionado.isEmpty()) {
                Toast.makeText(this, "Por favor selecciona un horario disponible", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Revisamos otra vez, por si alguien lo ocupó mientras tanto
            // (desde la app o desde la página web)
            btnConfirmar.isEnabled = false
            conBaseDeDatos {
                try {
                    if (BaseDeDatos.horarioOcupado(fechaSeleccionada, horarioSeleccionado)) {
                        Toast.makeText(this, "Ese horario ya está ocupado. Elige otro, por favor.", Toast.LENGTH_SHORT).show()
                        horarioSeleccionado = ""
                        actualizarHorarios()
                    } else if (reagendarId != -1L) {
                        reagendar()
                    } else {
                        irAlResumen()
                    }
                } finally {
                    btnConfirmar.isEnabled = true
                }
            }
        }
    }

    // Cada vez que se ve la pantalla (también al regresar del resumen) revisamos los horarios
    override fun onResume() {
        super.onResume()
        actualizarHorarios()
    }

    // Cita nueva: pasar a ResumenActivity los datos que llegaron de RegisterPetActivity,
    // más la fecha y el horario elegidos
    private fun irAlResumen() {
        val datos = intent // lo que mandó RegisterPetActivity
        val intentResumen = Intent(this, ResumenActivity::class.java).apply {
            putExtra("PROPIETARIO", datos.getStringExtra("PROPIETARIO"))
            putExtra("MASCOTA", datos.getStringExtra("MASCOTA"))
            putExtra("ESPECIE", datos.getStringExtra("ESPECIE"))
            putExtra("RAZA", datos.getStringExtra("RAZA"))
            putExtra("PESO", datos.getDoubleExtra("PESO", 0.0))
            putExtra("FOTO_URI", datos.getStringExtra("FOTO_URI"))
            putStringArrayListExtra("SERVICIOS", datos.getStringArrayListExtra("SERVICIOS"))
            putExtra("TOTAL", datos.getIntExtra("TOTAL", 0))
            putExtra("FECHA", fechaSeleccionada)
            putExtra("HORARIO", horarioSeleccionado)
        }
        startActivity(intentResumen)
    }

    // Cita rechazada: la misma cita cambia de día y hora y vuelve a estar pendiente
    private suspend fun reagendar() {
        val usuario = Sesion.usuario(this) ?: return

        if (BaseDeDatos.reagendarCita(reagendarId, usuario.id, fechaSeleccionada, horarioSeleccionado)) {
            Toast.makeText(this, "¡Cita reagendada! Ya está pendiente otra vez.", Toast.LENGTH_LONG).show()
            finish() // regresa a Mis Citas
        } else {
            Toast.makeText(this, "No se pudo reagendar: ese horario se acaba de ocupar. Elige otro.", Toast.LENGTH_SHORT).show()
            horarioSeleccionado = ""
            actualizarHorarios()
        }
    }

    // Pregunta a Firebase qué horarios ya están ocupados en el día elegido
    private fun actualizarHorarios() {
        val fechaPedida = fechaSeleccionada
        conBaseDeDatos {
            val ocupados = BaseDeDatos.obtenerHorariosOcupados(fechaPedida)
            // Si mientras tanto eligieron otro día, esta respuesta ya no sirve
            if (fechaPedida == fechaSeleccionada) {
                horariosOcupados = ocupados
                pintarHorarios()
            }
        }
    }

    // Pinta los 4 botones según su estado:
    //   ocupado (ya tiene cita) -> gris, con 🔴 y no se puede tocar
    //   elegido                 -> Wisteria Blue (#80A1D4)
    //   libre                   -> blanco
    private fun pintarHorarios() {
        for ((i, boton) in botonesHorarios.withIndex()) {
            val horario = horarios[i]
            when {
                horario in horariosOcupados -> {
                    boton.text = getString(R.string.horario_ocupado, horario)
                    boton.isEnabled = false
                    pintar(boton, "#E6E6E6", "#9E9E9E")
                }
                horario == horarioSeleccionado -> {
                    boton.text = getString(R.string.horario_disponible, horario)
                    boton.isEnabled = true
                    pintar(boton, "#80A1D4", "#1A103C")
                }
                else -> {
                    boton.text = getString(R.string.horario_disponible, horario)
                    boton.isEnabled = true
                    pintar(boton, "#FFFFFF", "#2C2C2C")
                }
            }
        }
    }

    // El fondo se cambia con backgroundTintList, igual que android:backgroundTint en el XML
    private fun pintar(boton: Button, fondo: String, texto: String) {
        boton.backgroundTintList = ColorStateList.valueOf(fondo.toColorInt())
        boton.setTextColor(texto.toColorInt())
    }

    // 5, 3, 2026 -> "2026-03-05" (año-mes-día con ceros, igual que en la página web;
    // así las fechas se pueden ordenar como texto)
    private fun formatearFecha(dia: Int, mes: Int, anio: Int): String {
        return String.format(Locale.US, "%04d-%02d-%02d", anio, mes, dia)
    }
}
