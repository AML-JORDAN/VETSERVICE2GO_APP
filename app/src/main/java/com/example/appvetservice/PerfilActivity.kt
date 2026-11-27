package com.example.appvetservice

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// PANTALLA DE PERFIL: muestra con qué cuenta se inició sesión y deja cerrarla.
class PerfilActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_perfil)

        // Los datos los guardó LoginActivity al iniciar sesión (ver Sesion.kt)
        val usuario = Sesion.usuario(this)

        val tvCorreo = findViewById<TextView>(R.id.tvCorreoPerfil)
        val tvCitas = findViewById<TextView>(R.id.tvCitasPerfil)
        if (usuario != null) {
            tvCorreo.text = getString(R.string.perfil_nombre_correo, usuario.nombre, usuario.correo)
        } else {
            tvCorreo.setText(R.string.perfil_sin_correo)
        }
        findViewById<TextView>(R.id.tvRolPerfil).setText(
            if (usuario?.rol == "empleado") R.string.perfil_empleado else R.string.perfil_cliente
        )

        // Solo las citas de esta cuenta
        if (usuario != null) {
            conBaseDeDatos {
                val citas = BaseDeDatos.obtenerCitasDeUsuario(usuario.id)
                tvCitas.text = getString(R.string.perfil_citas, citas.size)
            }
        }

        findViewById<Button>(R.id.btnPerfilMisCitas).setOnClickListener {
            startActivity(Intent(this, MisCitasActivity::class.java))
        }

        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            Sesion.cerrar(this)
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()

            // Volvemos al login y borramos el historial de pantallas,
            // para que con "atrás" no se pueda regresar sin iniciar sesión
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Navegación inferior
        findViewById<View>(R.id.navInicio).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        findViewById<View>(R.id.navMisCitas).setOnClickListener {
            startActivity(Intent(this, MisCitasActivity::class.java))
        }

        findViewById<View>(R.id.navRegistrar).setOnClickListener {
            startActivity(Intent(this, RegisterPetActivity::class.java))
        }

        findViewById<View>(R.id.navPerfil).setOnClickListener {
            // Ya estás en Perfil
        }
    }
}
