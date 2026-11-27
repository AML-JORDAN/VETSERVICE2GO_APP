package com.example.appvetservice

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvAvisoRechazadas: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        tvAvisoRechazadas = findViewById(R.id.tvAvisoRechazadas)

        findViewById<View>(R.id.btnRegistrarCita).setOnClickListener {
            startActivity(Intent(this, RegisterPetActivity::class.java))
        }

        findViewById<View>(R.id.btnMisCitas).setOnClickListener {
            startActivity(Intent(this, MisCitasActivity::class.java))
        }

        // El aviso de citas rechazadas lleva a Mis Citas, donde se reagendan
        tvAvisoRechazadas.setOnClickListener {
            startActivity(Intent(this, MisCitasActivity::class.java))
        }

        // Navegación inferior
        findViewById<View>(R.id.navInicio).setOnClickListener {
            // Ya estás en Inicio
        }

        findViewById<View>(R.id.navMisCitas).setOnClickListener {
            startActivity(Intent(this, MisCitasActivity::class.java))
        }

        findViewById<View>(R.id.navRegistrar).setOnClickListener {
            startActivity(Intent(this, RegisterPetActivity::class.java))
        }

        findViewById<View>(R.id.navPerfil).setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }
    }

    // Cada vez que se regresa a Inicio
    override fun onResume() {
        super.onResume()

        // Las fotos ya guardadas viven en Firebase: borramos las copias que quedaron en la app
        Fotos.borrarCopias(this)

        // Aviso de citas rechazadas (como en el panel de la página web)
        val usuario = Sesion.usuario(this) ?: return
        conBaseDeDatos {
            val citas = BaseDeDatos.obtenerCitasDeUsuario(usuario.id)
            val rechazadas = citas.count { it.estado == BaseDeDatos.ESTADO_RECHAZADO }
            if (rechazadas > 0) {
                tvAvisoRechazadas.text = resources.getQuantityString(R.plurals.aviso_rechazadas, rechazadas, rechazadas)
                tvAvisoRechazadas.visibility = View.VISIBLE
            } else {
                tvAvisoRechazadas.visibility = View.GONE
            }
        }
    }
}
