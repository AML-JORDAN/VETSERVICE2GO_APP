package com.example.appvetservice

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// MIS CITAS: solo las citas de quien inició sesión, con su estado.
// Si un empleado rechazó una, aquí aparece el botón para reagendarla.
class MisCitasActivity : AppCompatActivity() {

    private lateinit var rvMisCitas: RecyclerView
    private lateinit var tvSinCitas: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_mis_citas)

        rvMisCitas = findViewById(R.id.rvMisCitas)
        tvSinCitas = findViewById(R.id.tvSinCitas)
        rvMisCitas.layoutManager = LinearLayoutManager(this)

        // Navegación inferior
        findViewById<View>(R.id.navInicio).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        findViewById<View>(R.id.navMisCitas).setOnClickListener {
            // Ya estás en Mis Citas
        }

        findViewById<View>(R.id.navRegistrar).setOnClickListener {
            startActivity(Intent(this, RegisterPetActivity::class.java))
        }

        findViewById<View>(R.id.navPerfil).setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }
    }

    // Se vuelve a cargar cada vez que se ve la pantalla (por ejemplo, al regresar de reagendar)
    override fun onResume() {
        super.onResume()
        cargarCitas()
    }

    private fun cargarCitas() {
        val usuario = Sesion.usuario(this) ?: return
        conBaseDeDatos {
            val citas = BaseDeDatos.obtenerCitasDeUsuario(usuario.id)

            if (citas.isEmpty()) {
                tvSinCitas.visibility = View.VISIBLE
                rvMisCitas.visibility = View.GONE
            } else {
                tvSinCitas.visibility = View.GONE
                rvMisCitas.visibility = View.VISIBLE
            }

            rvMisCitas.adapter = CitasAdapter(citas, esEmpleado = false, alReagendar = { cita -> reagendar(cita) })
        }
    }

    // Abre la pantalla de fecha y hora en modo "reagendar" para esa cita
    private fun reagendar(cita: CitaItem) {
        val intent = Intent(this, FechaHoraActivity::class.java)
        intent.putExtra("REAGENDAR_ID", cita.id)
        intent.putExtra("MASCOTA", cita.mascota)
        startActivity(intent)
    }
}
