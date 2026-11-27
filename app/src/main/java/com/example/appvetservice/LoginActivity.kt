package com.example.appvetservice

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_login)

        val etCorreo = findViewById<EditText>(R.id.etCorreoLogin)
        val etPassword = findViewById<EditText>(R.id.etPasswordLogin)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnIrRegistro = findViewById<Button>(R.id.btnIrRegistro)

        btnLogin.setOnClickListener {
            // El correo se compara en minúsculas: Jordan@gmail.com = jordan@gmail.com
            val correo = etCorreo.text.toString().trim().lowercase()
            val password = etPassword.text.toString().trim()

            if (correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa correo/usuario y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // "empleado" es un atajo para el correo del empleado por defecto
            val correoParaValidar = if (correo == "empleado") {
                BaseDeDatos.EMPLEADO_CORREO_DEFAULT
            } else {
                correo
            }

            // Mientras Firebase responde, el botón no se puede volver a presionar
            btnLogin.isEnabled = false
            conBaseDeDatos {
                try {
                    val usuario = BaseDeDatos.buscarUsuario(correoParaValidar, password)
                    entrar(usuario)
                } finally {
                    btnLogin.isEnabled = true
                }
            }
        }

        btnIrRegistro.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // Cada tipo de cuenta tiene su propia pantalla
    private fun entrar(usuario: Usuario?) {
        if (usuario == null) {
            Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
            return
        }

        when (usuario.rol) {
            "admin" -> {
                // El super usuario administra las cuentas desde la página web
                Toast.makeText(this, "La cuenta de super usuario se usa en la página web", Toast.LENGTH_LONG).show()
            }
            "empleado" -> {
                Sesion.guardar(this, usuario)
                Toast.makeText(this, "Sesión iniciada como Empleado", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, EmpleadoActivity::class.java))
                finish()
            }
            else -> {
                Sesion.guardar(this, usuario)
                Toast.makeText(this, "Sesión iniciada como Cliente", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}
