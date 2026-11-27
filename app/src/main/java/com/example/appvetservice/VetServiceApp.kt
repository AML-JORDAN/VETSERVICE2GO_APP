package com.example.appvetservice

import android.app.Application

// Se crea al abrir la app, antes que cualquier pantalla (está en AndroidManifest.xml).
// Aquí nos conectamos con Firebase una sola vez.
class VetServiceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        BaseDeDatos.iniciar(this)
    }
}
