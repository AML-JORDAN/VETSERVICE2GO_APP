package com.example.appvetservice

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView

// Lista de citas. La usan el cliente (Mis Citas) y el empleado (EmpleadoActivity).
// esEmpleado cambia qué se ve y qué botones aparecen.
class CitasAdapter(
    private val citas: List<CitaItem>,
    private val esEmpleado: Boolean,
    private val alCompletar: (CitaItem) -> Unit = {},
    private val alRechazar: (CitaItem) -> Unit = {},
    private val alReagendar: (CitaItem) -> Unit = {}
) : RecyclerView.Adapter<CitasAdapter.CitaViewHolder>() {

    class CitaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivMascota: ImageView = view.findViewById(R.id.ivMascotaItem)
        val tvTitulo: TextView = view.findViewById(R.id.tvTituloMascota)
        val tvPrecio: TextView = view.findViewById(R.id.tvPrecioItem)
        val tvFecha: TextView = view.findViewById(R.id.tvFechaItem)
        val tvPropietario: TextView = view.findViewById(R.id.tvPropietarioItem)
        val tvCuenta: TextView = view.findViewById(R.id.tvCuentaItem)
        val tvEstado: TextView = view.findViewById(R.id.tvEstadoItem)
        val btnCompletar: Button = view.findViewById(R.id.btnCompletarItem)
        val btnRechazar: Button = view.findViewById(R.id.btnRechazarItem)
        val btnReagendar: Button = view.findViewById(R.id.btnReagendarItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CitaViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cita_card, parent, false)
        return CitaViewHolder(vista)
    }

    override fun onBindViewHolder(holder: CitaViewHolder, position: Int) {
        val cita = citas[position]
        val contexto = holder.itemView.context

        val especieTexto = if (cita.especie.isNotBlank()) " (${cita.especie})" else ""
        val serviciosTexto = cita.servicios.ifBlank { "Servicio veterinario" }
        val fechaTexto = if (cita.fechaCita.isNotBlank()) {
            "${BaseDeDatos.mostrarFecha(cita.fechaCita)} ${cita.horario}"
        } else {
            contexto.getString(R.string.sin_fecha)
        }

        holder.tvTitulo.text = contexto.getString(R.string.cita_titulo, cita.mascota, especieTexto, serviciosTexto)
        holder.tvPrecio.text = contexto.getString(R.string.cita_precio, cita.total)
        holder.tvFecha.text = contexto.getString(R.string.cita_fecha, fechaTexto)
        // Las citas agendadas en la página web traen como propietario el nombre de la cuenta
        holder.tvPropietario.text = contexto.getString(R.string.cita_propietario, cita.propietario.ifBlank { cita.cliente })

        // El empleado ve de qué cuenta es cada cita
        if (esEmpleado) {
            holder.tvCuenta.visibility = View.VISIBLE
            holder.tvCuenta.text = contexto.getString(R.string.cita_cuenta, cita.cliente)
        } else {
            holder.tvCuenta.visibility = View.GONE
        }

        mostrarEstado(holder, cita)
        mostrarFoto(holder, cita)
    }

    override fun getItemCount(): Int = citas.size

    // Texto del estado y los botones que tocan:
    //   pagado (pendiente) -> el empleado puede completarla o rechazarla
    //   rechazado          -> el cliente la puede reagendar
    //   completado         -> sin botones
    private fun mostrarEstado(holder: CitaViewHolder, cita: CitaItem) {
        val contexto = holder.itemView.context

        // Primero escondemos todos los botones (la lista reutiliza las tarjetas)
        holder.btnCompletar.visibility = View.GONE
        holder.btnRechazar.visibility = View.GONE
        holder.btnReagendar.visibility = View.GONE

        when (cita.estado) {
            BaseDeDatos.ESTADO_COMPLETADO -> {
                holder.tvEstado.text = contexto.getString(R.string.cita_completada, cita.atendidoPor, cita.completadoEn)
                holder.tvEstado.setTextColor("#2B7F7E".toColorInt())
            }
            BaseDeDatos.ESTADO_RECHAZADO -> {
                val texto = if (esEmpleado) R.string.cita_rechazada_empleado else R.string.cita_rechazada_cliente
                holder.tvEstado.text = contexto.getString(texto, cita.atendidoPor, cita.motivoRechazo)
                holder.tvEstado.setTextColor("#A32F2F".toColorInt())
                if (!esEmpleado) {
                    holder.btnReagendar.visibility = View.VISIBLE
                }
            }
            else -> {
                holder.tvEstado.setText(R.string.cita_pendiente)
                holder.tvEstado.setTextColor("#B7791F".toColorInt())
                if (esEmpleado) {
                    holder.btnCompletar.visibility = View.VISIBLE
                    holder.btnRechazar.visibility = View.VISIBLE
                }
            }
        }

        holder.btnCompletar.setOnClickListener { alCompletar(cita) }
        holder.btnRechazar.setOnClickListener { alRechazar(cita) }
        holder.btnReagendar.setOnClickListener { alReagendar(cita) }
    }

    // Foto de la mascota (viene de Firebase como texto, ver Fotos.kt):
    // sin el tinte gris del ícono y sin margen
    private fun mostrarFoto(holder: CitaViewHolder, cita: CitaItem) {
        val imagen = Fotos.comoImagen(cita.foto)
        if (imagen != null) {
            holder.ivMascota.imageTintList = null
            holder.ivMascota.setPadding(0, 0, 0, 0)
            holder.ivMascota.setImageBitmap(imagen)
        } else {
            mostrarIcono(holder) // no tiene foto, o no se pudo leer
        }
    }

    // Ícono de cámara gris con margen (como viene en item_cita_card.xml).
    // Hay que ponerlo completo porque la lista reutiliza las tarjetas.
    private fun mostrarIcono(holder: CitaViewHolder) {
        val margen = (10 * holder.itemView.resources.displayMetrics.density).toInt() // 10dp en pixeles
        holder.ivMascota.setPadding(margen, margen, margen, margen)
        holder.ivMascota.imageTintList = ColorStateList.valueOf("#757575".toColorInt())
        holder.ivMascota.setImageResource(android.R.drawable.ic_menu_camera)
    }
}
