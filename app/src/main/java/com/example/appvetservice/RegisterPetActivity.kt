package com.example.appvetservice

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

class RegisterPetActivity : AppCompatActivity() {

    private lateinit var ivFotoMascota: ImageView
    private var imagenUri: Uri? = null

    // Lanzador oficial para abrir la galería sin errores
    private val seleccionarImagenLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // Guardamos una copia más pequeña dentro de la app (ver Fotos.kt)
            val copia = Fotos.copiarReducida(this, uri)
            if (copia != null) {
                imagenUri = copia
                ivFotoMascota.setImageURI(copia)
                Toast.makeText(this, "Foto de mascota cargada", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No se pudo cargar la foto", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No se seleccionó ninguna imagen", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_pet)

        // Ocultar la barra superior predeterminada para que coincida exactamente con la imagen
        supportActionBar?.hide()

        val etPropietario = findViewById<EditText>(R.id.etNombrePropietario)
        val etMascota = findViewById<EditText>(R.id.etNombreMascota)
        val etEspecie = findViewById<EditText>(R.id.etEspecieMascota)
        val etRaza = findViewById<EditText>(R.id.etRaza)
        val etPeso = findViewById<EditText>(R.id.etPesoMascota)
        val btnSeleccionarFoto = findViewById<Button>(R.id.btnSeleccionarFoto)
        val btnFechaHora = findViewById<Button>(R.id.btnSeleccionarFechaHora)
        ivFotoMascota = findViewById(R.id.ivFotoMascota)

        // Al girar el teléfono Android vuelve a crear la pantalla:
        // recuperamos la foto que ya se había elegido
        savedInstanceState?.getString("FOTO_URI")?.let { guardada ->
            imagenUri = guardada.toUri()
            ivFotoMascota.setImageURI(imagenUri)
        }

        // Cada casilla con su servicio. El texto y el precio salen de Servicios.kt
        val opciones = listOf(
            findViewById<CheckBox>(R.id.cbConsulta) to Servicios.CONSULTA,
            findViewById<CheckBox>(R.id.cbVacunacion) to Servicios.VACUNACION,
            findViewById<CheckBox>(R.id.cbDesparasitacion) to Servicios.DESPARASITACION,
            findViewById<CheckBox>(R.id.cbBano) to Servicios.BANO,
            findViewById<CheckBox>(R.id.cbUrgencias) to Servicios.URGENCIAS
        )
        for ((casilla, servicio) in opciones) {
            casilla.text = Servicios.conPrecio(servicio)
        }

        // Botón para abrir la galería al presionar "SUBIR FOTO DE MASCOTA"
        btnSeleccionarFoto.setOnClickListener {
            try {
                seleccionarImagenLauncher.launch("image/*")
            } catch (e: Exception) {
                Toast.makeText(this, "Error al abrir la galería: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Botón para continuar a la siguiente pantalla
        btnFechaHora.setOnClickListener {
            val propietario = etPropietario.text.toString().trim()
            val mascota = etMascota.text.toString().trim()
            val especie = etEspecie.text.toString().trim()
            val raza = etRaza.text.toString().trim()
            // Algunos teclados escriben el punto decimal como coma: "4,5" -> "4.5"
            val peso = etPeso.text.toString().trim().replace(',', '.').toDoubleOrNull()

            if (propietario.isEmpty() || mascota.isEmpty()) {
                Toast.makeText(this, "Por favor llena al menos el propietario y la mascota", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // El peso se pide igual que en la página web
            if (peso == null || peso <= 0) {
                Toast.makeText(this, "Escribe el peso de la mascota en kg (mayor a 0)", Toast.LENGTH_SHORT).show()
                etPeso.requestFocus()
                return@setOnClickListener
            }

            // Servicios marcados
            val elegidos = opciones.filter { (casilla, _) -> casilla.isChecked }.map { (_, servicio) -> servicio }
            if (elegidos.isEmpty()) {
                Toast.makeText(this, "Selecciona al menos un servicio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // El total se suma con los precios de Servicios.kt
            val total = elegidos.sumOf { Servicios.precio(it) }

            // Enviar datos a la siguiente actividad de fecha y hora
            val intent = Intent(this, FechaHoraActivity::class.java).apply {
                putExtra("PROPIETARIO", propietario)
                putExtra("MASCOTA", mascota)
                putExtra("ESPECIE", especie)
                putExtra("RAZA", raza)
                putExtra("PESO", peso)
                putExtra("FOTO_URI", imagenUri?.toString())
                putStringArrayListExtra("SERVICIOS", ArrayList(elegidos))
                putExtra("TOTAL", total)
            }
            startActivity(intent)
        }
    }

    // Antes de que Android destruya la pantalla (por ejemplo, al girar el teléfono)
    // guardamos la foto elegida para recuperarla en onCreate
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("FOTO_URI", imagenUri?.toString())
    }
}
