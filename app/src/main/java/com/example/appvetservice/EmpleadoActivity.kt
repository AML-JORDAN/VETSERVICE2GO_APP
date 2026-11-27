package com.example.appvetservice

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// PANTALLA DEL EMPLEADO (como empleado.html en la página web):
// ve las citas de todos los clientes (las de la app y las de la página)
// y las marca como completadas o las rechaza.
// Si la rechaza, el cliente tiene que reagendarla (en la app o en la página).
class EmpleadoActivity : AppCompatActivity() {

    private lateinit var rvTickets: RecyclerView
    private lateinit var tvSinTickets: TextView
    private lateinit var botonesFiltro: Map<String, TextView>

    // Qué lista se está viendo: "pagado", "completado" o "rechazado"
    private var estadoVisible = BaseDeDatos.ESTADO_PAGADO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_empleado)

        rvTickets = findViewById(R.id.rvTickets)
        tvSinTickets = findViewById(R.id.tvSinTickets)
        rvTickets.layoutManager = LinearLayoutManager(this)

        findViewById<TextView>(R.id.tvHolaEmpleado).text = getString(R.string.empleado_hola, nombreEmpleado())

        // Un filtro por cada estado
        botonesFiltro = mapOf(
            BaseDeDatos.ESTADO_PAGADO to findViewById(R.id.btnFiltroPendientes),
            BaseDeDatos.ESTADO_COMPLETADO to findViewById(R.id.btnFiltroCompletadas),
            BaseDeDatos.ESTADO_RECHAZADO to findViewById(R.id.btnFiltroRechazadas)
        )
        for ((estado, filtro) in botonesFiltro) {
            filtro.setOnClickListener {
                estadoVisible = estado
                cargarTickets()
            }
        }

        // Cerrar sesión: volvemos al login y borramos el historial de pantallas
        findViewById<Button>(R.id.btnCerrarSesionEmpleado).setOnClickListener {
            Sesion.cerrar(this)
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        cargarTickets()
    }

    // Pide a Firebase todas las citas, muestra las del estado elegido y cuenta las de cada estado
    private fun cargarTickets() {
        conBaseDeDatos {
            val todas = BaseDeDatos.obtenerTodasLasCitas()

            // Las pendientes van de la más cercana a la más lejana;
            // las demás al revés, para ver primero las más recientes (como en la página web)
            var citas = todas.filter { it.estado == estadoVisible }
            if (estadoVisible != BaseDeDatos.ESTADO_PAGADO) {
                citas = citas.reversed()
            }

            rvTickets.adapter = CitasAdapter(
                citas,
                esEmpleado = true,
                alCompletar = { cita -> confirmarCompletar(cita) },
                alRechazar = { cita -> pedirMotivo(cita) }
            )
            tvSinTickets.visibility = if (citas.isEmpty()) View.VISIBLE else View.GONE

            // Cuántas citas hay de cada estado
            val textos = mapOf(
                BaseDeDatos.ESTADO_PAGADO to R.string.filtro_pendientes,
                BaseDeDatos.ESTADO_COMPLETADO to R.string.filtro_completadas,
                BaseDeDatos.ESTADO_RECHAZADO to R.string.filtro_rechazadas
            )
            for ((estado, filtro) in botonesFiltro) {
                filtro.text = getString(textos.getValue(estado), todas.count { it.estado == estado })
            }

            pintarFiltros()
        }
    }

    // El filtro elegido va en azul; los demás en blanco con borde azul
    private fun pintarFiltros() {
        for ((estado, filtro) in botonesFiltro) {
            if (estado == estadoVisible) {
                filtro.setBackgroundResource(R.drawable.fondo_filtro_elegido)
                filtro.setTextColor("#FFFFFF".toColorInt())
            } else {
                filtro.setBackgroundResource(R.drawable.fondo_filtro)
                filtro.setTextColor("#17346F".toColorInt())
            }
        }
    }

    // Antes de completarla preguntamos (como confirm() en la web)
    private fun confirmarCompletar(cita: CitaItem) {
        AlertDialog.Builder(this)
            .setTitle("Marcar como completada")
            .setMessage("¿Ya diste el servicio de ${cita.mascota} (${BaseDeDatos.mostrarFecha(cita.fechaCita)} ${cita.horario})?")
            .setPositiveButton("Sí, completada") { _, _ ->
                conBaseDeDatos {
                    BaseDeDatos.completarCita(cita.id, nombreEmpleado())
                    Toast.makeText(this, "Cita completada", Toast.LENGTH_SHORT).show()
                    cargarTickets()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Una ventana con una caja de texto para escribir el motivo (como prompt() en la web)
    private fun pedirMotivo(cita: CitaItem) {
        val campoMotivo = EditText(this).apply {
            setHint(R.string.motivo_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        val margen = (20 * resources.displayMetrics.density).toInt() // 20dp en pixeles
        val contenedor = FrameLayout(this).apply {
            setPadding(margen, margen / 2, margen, 0)
            addView(campoMotivo)
        }

        AlertDialog.Builder(this)
            .setTitle("Rechazar la cita de ${cita.mascota}")
            .setMessage("El cliente verá el motivo y tendrá que reagendar.")
            .setView(contenedor)
            .setPositiveButton("Rechazar") { _, _ ->
                val motivo = campoMotivo.text.toString().trim()
                if (motivo.isEmpty()) {
                    Toast.makeText(this, "Escribe el motivo para que el cliente sepa por qué se rechazó.", Toast.LENGTH_LONG).show()
                } else {
                    conBaseDeDatos {
                        BaseDeDatos.rechazarCita(cita.id, nombreEmpleado(), motivo)
                        Toast.makeText(this, "Cita rechazada", Toast.LENGTH_SHORT).show()
                        cargarTickets()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // El nombre del empleado queda guardado en la cita (como en la página web)
    private fun nombreEmpleado(): String {
        val usuario = Sesion.usuario(this)
        return usuario?.nombre?.ifBlank { usuario.correo } ?: "Empleado"
    }
}
