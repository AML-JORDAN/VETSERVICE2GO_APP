package com.example.appvetservice

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

class ResumenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_resumen)

        // Referencias a los elementos visuales de la tarjeta
        val ivMascota = findViewById<ImageView>(R.id.ivMascotaResumen)
        val tvPropietario = findViewById<TextView>(R.id.tvPropietarioResumen)
        val tvMascota = findViewById<TextView>(R.id.tvMascotaResumen)
        val tvFecha = findViewById<TextView>(R.id.tvFechaResumen)
        val tvServicios = findViewById<TextView>(R.id.tvServiciosResumen)
        val tvTotal = findViewById<TextView>(R.id.tvTotalResumen)
        val btnFinalizar = findViewById<Button>(R.id.btnFinalizar)

        // Recibir los datos enviados desde las pantallas anteriores
        val propietario = intent.getStringExtra("PROPIETARIO") ?: "No especificado"
        val mascota = intent.getStringExtra("MASCOTA") ?: "No especificado"
        val especie = intent.getStringExtra("ESPECIE") ?: ""
        val raza = intent.getStringExtra("RAZA") ?: ""
        val peso = intent.getDoubleExtra("PESO", 0.0)
        val fecha = intent.getStringExtra("FECHA") ?: ""       // "2026-11-27"
        val horario = intent.getStringExtra("HORARIO") ?: ""
        val fotoUri = intent.getStringExtra("FOTO_URI")
        val servicios = intent.getStringArrayListExtra("SERVICIOS") ?: arrayListOf()

        // El total ya viene calculado (con los precios de Servicios.kt)
        val total = intent.getIntExtra("TOTAL", 0)

        // Mostrar la información en la tarjeta (los servicios con su precio)
        tvPropietario.text = getString(R.string.resumen_propietario, propietario)
        tvMascota.text = getString(R.string.resumen_mascota, mascota)
        tvFecha.text = getString(R.string.resumen_fecha, BaseDeDatos.mostrarFecha(fecha), horario)
        tvServicios.text = getString(
            R.string.resumen_servicios,
            servicios.joinToString(", ") { Servicios.conPrecio(it) }
        )
        tvTotal.text = getString(R.string.resumen_total, total)

        // Foto de la mascota, si se subió una
        if (!fotoUri.isNullOrBlank()) {
            ivMascota.setImageURI(fotoUri.toUri())
            if (ivMascota.drawable != null) {
                ivMascota.visibility = View.VISIBLE
            }
        }

        // Botón para finalizar: guardar en Firebase y volver al inicio
        btnFinalizar.setOnClickListener {
            // La cita se guarda a nombre de quien inició sesión
            val usuario = Sesion.usuario(this)
            if (usuario == null) {
                Toast.makeText(this, "Vuelve a iniciar sesión, por favor", Toast.LENGTH_SHORT).show()
                irA(LoginActivity::class.java)
                return@setOnClickListener
            }

            btnFinalizar.isEnabled = false
            conBaseDeDatos {
                try {
                    // En la base de datos los servicios van solo con su nombre,
                    // igual que en la página web: "Consulta General, Vacunación"
                    val folio = BaseDeDatos.crearCita(
                        usuario = usuario,
                        propietario = propietario,
                        mascota = mascota,
                        especie = especie,
                        raza = raza,
                        peso = peso,
                        foto = Fotos.comoTexto(this, fotoUri),
                        servicios = servicios.joinToString(", "),
                        total = total,
                        fechaCita = fecha,
                        horario = horario
                    )

                    if (folio == null) {
                        // Alguien ganó ese horario mientras tanto
                        Toast.makeText(this, "Ese horario se acaba de ocupar. Elige otro, por favor.", Toast.LENGTH_LONG).show()
                        finish() // regresa a la pantalla de fecha y hora
                    } else {
                        Toast.makeText(this, "¡Cita guardada! Folio #${folio.toString().padStart(4, '0')}", Toast.LENGTH_SHORT).show()
                        irA(MainActivity::class.java)
                    }
                } finally {
                    btnFinalizar.isEnabled = true
                }
            }
        }
    }

    // Abre otra pantalla limpiando el historial (con "atrás" ya no se regresa aquí)
    private fun irA(pantalla: Class<*>) {
        val intent = Intent(this, pantalla)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
