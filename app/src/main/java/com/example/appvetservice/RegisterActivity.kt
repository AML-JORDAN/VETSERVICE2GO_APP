package com.example.appvetservice

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_register)

        val etNombre = findViewById<EditText>(R.id.etNombreRegistro)
        val etCorreo = findViewById<EditText>(R.id.etCorreoRegistro)
        val etPassword = findViewById<EditText>(R.id.etPasswordRegistro)
        val btnRegistrarse = findViewById<Button>(R.id.btnRegistrarse)
        val btnVolverLogin = findViewById<Button>(R.id.btnVolverLogin)

        btnRegistrarse.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val correo = etCorreo.text.toString().trim().lowercase()
            val password = etPassword.text.toString().trim()

            if (nombre.isEmpty() || correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Patterns.EMAIL_ADDRESS revisa que el correo tenga la forma nombre@dominio.com
            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                Toast.makeText(this, "Escribe un correo válido, por ejemplo nombre@gmail.com", Toast.LENGTH_SHORT).show()
                etCorreo.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 4) {
                Toast.makeText(this, "La contraseña debe tener al menos 4 caracteres", Toast.LENGTH_SHORT).show()
                etPassword.requestFocus()
                return@setOnClickListener
            }

            // Registrar la cuenta en Firebase (la misma base de datos de la página web)
            btnRegistrarse.isEnabled = false
            conBaseDeDatos {
                try {
                    if (BaseDeDatos.registrarUsuario(nombre, correo, password)) {
                        Toast.makeText(this, "¡Usuario registrado con éxito! Ahora inicia sesión.", Toast.LENGTH_SHORT).show()
                        finish() // regresa al login
                    } else {
                        Toast.makeText(this, "Error: Este correo ya está registrado", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    btnRegistrarse.isEnabled = true
                }
            }
        }

        btnVolverLogin.setOnClickListener {
            finish() // regresa al login
        }
    }
}
