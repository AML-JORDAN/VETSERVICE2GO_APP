package com.example.appvetservice

import android.content.Context
import androidx.core.content.edit

// Recuerda quién inició sesión (su id, nombre, correo y rol) aunque se cambie de pantalla.
// SharedPreferences es un archivo pequeño de "llave = valor" que guarda la app.
object Sesion {

    private const val ARCHIVO = "sesion"

    fun guardar(context: Context, usuario: Usuario) {
        context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).edit {
            putString("id", usuario.id)
            putString("nombre", usuario.nombre)
            putString("correo", usuario.correo)
            putString("rol", usuario.rol)
        }
    }

    // Quien inició sesión, o null si nadie (o si la sesión es de una versión anterior de la app)
    fun usuario(context: Context): Usuario? {
        val datos = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
        val id = datos.getString("id", null) ?: return null
        return Usuario(
            id = id,
            nombre = datos.getString("nombre", null) ?: "",
            correo = datos.getString("correo", null) ?: "",
            rol = datos.getString("rol", null) ?: "cliente"
        )
    }

    // Borra los datos de la sesión (al cerrar sesión)
    fun cerrar(context: Context) {
        context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).edit { clear() }
    }
}
