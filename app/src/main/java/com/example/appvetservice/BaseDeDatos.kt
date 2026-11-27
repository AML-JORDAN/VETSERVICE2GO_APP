package com.example.appvetservice

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================
// BASE DE DATOS (Firebase Cloud Firestore)
//
// La app y la página web usan la MISMA base de datos, que vive en internet,
// en Firebase. Una cita que se agenda aquí la ve el empleado en la página web,
// y al revés. Este archivo hace lo mismo que js/basedatos.js en la página.
//
// Firestore no tiene tablas sino "colecciones" de "documentos".
// Un documento es como un renglón: tiene un id y sus datos ("campos").
//
// Colecciones (los campos se llaman igual que en la página web):
//   usuarios  (nombre, correo, password, rol)
//   mascotas  (usuario_id, nombre, especie, raza, peso, foto)
//   citas     Cada cita es un recibo. Su id es el folio: 1, 2, 3...
//             (usuario_id, cliente, propietario, mascota_id, mascota, especie, raza,
//              peso, foto, servicios, total, fecha_cita, horario, creado,
//              estado, atendido_por, completado_en, motivo_rechazo)
//   horarios  Un documento por cada horario ocupado. Su id es el día y la hora
//             ("2026-11-27 15:30 - 16:10"), así no puede haber dos citas a la misma hora.
//   config    "folios" (el último folio que se usó)
//
// Las fechas se guardan como "2026-11-27" (año-mes-día), igual que en la página.
//
// Hablar con Firebase tarda un poquito (va por internet). Por eso estas funciones
// son "suspend": se llaman desde conBaseDeDatos { ... } (al final de este archivo)
// y la pantalla no se congela mientras llega la respuesta.
// ============================================

// Quien inició sesión
data class Usuario(val id: String, val nombre: String, val correo: String, val rol: String)

object BaseDeDatos {

    // Estados de una cita (los mismos de la página web):
    //   "pagado"     se acaba de agendar y espera a que un empleado la atienda
    //   "completado" el empleado ya dio el servicio
    //   "rechazado"  el empleado no la pudo atender; el cliente la tiene que reagendar
    const val ESTADO_PAGADO = "pagado"
    const val ESTADO_COMPLETADO = "completado"
    const val ESTADO_RECHAZADO = "rechazado"

    // Cuenta del empleado por defecto (en el login basta con escribir "empleado")
    const val EMPLEADO_CORREO_DEFAULT = "empleado@vetservice2go.com"

    // La conexión con Firestore (Firebase la guarda; aquí solo la pedimos)
    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    // Se conecta con el proyecto "vetservice2go" de Firebase.
    // Lo llama VetServiceApp al abrir la app, antes que cualquier pantalla.
    // Son los mismos datos de FIREBASE_CONFIG en js/basedatos.js (no son una contraseña).
    fun iniciar(context: Context) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            val opciones = FirebaseOptions.Builder()
                .setProjectId("vetservice2go")
                .setApplicationId("1:913020484601:web:c1e427955ec60e5c8f5b28")
                .setApiKey("AIzaSyBAr0q8O_hTvAV3iqSVBPbrR0YVo5BwzI0")
                .setGcmSenderId("913020484601")
                .setStorageBucket("vetservice2go.firebasestorage.app")
                .build()
            FirebaseApp.initializeApp(context, opciones)
        }
    }

    // ============================================
    // USUARIOS
    // ============================================

    // Iniciar sesión: regresa el usuario, o null si el correo y la contraseña no coinciden
    suspend fun buscarUsuario(correo: String, password: String): Usuario? {
        val encontrados = db.collection("usuarios").whereEqualTo("correo", correo).get().await()
        val doc = encontrados.documents.firstOrNull() ?: return null

        if (doc.getString("password") != cifrar(password)) {
            return null
        }
        return Usuario(
            id = doc.id,
            nombre = doc.getString("nombre") ?: "",
            correo = correo,
            rol = doc.getString("rol") ?: "cliente"
        )
    }

    // Registrar una cuenta de cliente. Regresa false si ese correo ya existe.
    suspend fun registrarUsuario(nombre: String, correo: String, password: String): Boolean {
        val existentes = db.collection("usuarios").whereEqualTo("correo", correo).get().await()
        if (!existentes.isEmpty) {
            return false
        }

        // add() agrega un documento nuevo; Firestore le pone un id solo
        db.collection("usuarios").add(
            mapOf(
                "nombre" to nombre,
                "correo" to correo,
                "password" to cifrar(password),
                "rol" to "cliente"
            )
        ).await()
        return true
    }

    // La contraseña no se guarda tal cual: se guarda su "huella" SHA-256,
    // un texto que sale de la contraseña pero con el que no se puede adivinar.
    // La página web calcula la misma huella, por eso las cuentas sirven en los dos lados.
    private fun cifrar(password: String): String {
        val huella = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        // Cada byte se escribe como 2 letras/números: 255 -> "ff"
        return huella.joinToString("") { "%02x".format(it) }
    }

    // ============================================
    // CITAS (lo que usa el cliente)
    // ============================================

    // Guardar una cita nueva. Empieza como "pagado".
    // Regresa el folio de la cita, o null si alguien ganó ese horario.
    suspend fun crearCita(
        usuario: Usuario,
        propietario: String,
        mascota: String,
        especie: String,
        raza: String,
        peso: Double,
        foto: String,
        servicios: String,
        total: Int,
        fechaCita: String,
        horario: String
    ): Long? {
        // La mascota también queda registrada, así aparece en el panel de la página web
        val (mascotaId, fotoFinal) = guardarMascota(usuario.id, mascota, especie, raza, peso, foto)

        val refHorario = db.collection("horarios").document(idDeHorario(fechaCita, horario))
        val refFolios = db.collection("config").document("folios")

        // Una "transacción" lee y escribe de un solo golpe: revisamos que el horario
        // siga libre, sacamos el folio que sigue y guardamos la cita, todo junto.
        return db.runTransaction<Long?> { transaccion ->
            if (transaccion.get(refHorario).exists()) {
                return@runTransaction null // alguien lo ocupó
            }
            val folio = (transaccion.get(refFolios).getLong("ultimo") ?: 0L) + 1

            transaccion.set(refFolios, mapOf("ultimo" to folio))
            transaccion.set(
                refHorario,
                mapOf("fecha_cita" to fechaCita, "horario" to horario, "cita_id" to folio)
            )
            transaccion.set(
                db.collection("citas").document(folio.toString()),
                mapOf(
                    "usuario_id" to usuario.id,
                    "cliente" to usuario.nombre,
                    "propietario" to propietario,
                    "mascota_id" to mascotaId,
                    "mascota" to mascota,
                    "especie" to especie,
                    "raza" to raza,
                    "peso" to peso,
                    "foto" to fotoFinal,
                    "servicios" to servicios,
                    "total" to total,
                    "fecha_cita" to fechaCita,
                    "horario" to horario,
                    "creado" to ahora(),
                    "estado" to ESTADO_PAGADO,
                    "atendido_por" to null,
                    "completado_en" to null,
                    "motivo_rechazo" to null
                )
            )
            folio
        }.await()
    }

    // Si el cliente ya tiene una mascota con ese nombre (registrada aquí o en la página),
    // se actualizan sus datos; si no, se crea. Regresa su id y la foto que quedó.
    private suspend fun guardarMascota(
        usuarioId: String,
        nombre: String,
        especie: String,
        raza: String,
        peso: Double,
        foto: String
    ): Pair<String, String> {
        val suyas = db.collection("mascotas").whereEqualTo("usuario_id", usuarioId).get().await()
        val misma = suyas.documents.firstOrNull { it.getString("nombre").equals(nombre, ignoreCase = true) }

        // Si ahora no eligieron foto, se queda la que ya tenía
        val fotoFinal = foto.ifBlank { misma?.getString("foto") ?: "" }
        val datos = mapOf(
            "usuario_id" to usuarioId,
            "nombre" to nombre,
            "especie" to especie,
            "raza" to raza,
            "peso" to peso,
            "foto" to fotoFinal
        )

        if (misma != null) {
            misma.reference.set(datos).await()
            return Pair(misma.id, fotoFinal)
        }
        val nueva = db.collection("mascotas").add(datos).await()
        return Pair(nueva.id, fotoFinal)
    }

    // Las citas de un cliente, las más nuevas primero
    suspend fun obtenerCitasDeUsuario(usuarioId: String): List<CitaItem> {
        val resultado = db.collection("citas").whereEqualTo("usuario_id", usuarioId).get().await()
        return resultado.documents.map { comoCita(it) }.sortedByDescending { it.id }
    }

    // El cliente reagenda una cita rechazada. Es la MISMA cita: solo cambia el día
    // y la hora, y vuelve a quedar "pagado". Su horario anterior queda libre.
    // Regresa false si no se pudo (por ejemplo, si el horario nuevo ya está ocupado).
    suspend fun reagendarCita(citaId: Long, usuarioId: String, fechaCita: String, horario: String): Boolean {
        val refCita = db.collection("citas").document(citaId.toString())
        val refNuevo = db.collection("horarios").document(idDeHorario(fechaCita, horario))

        return db.runTransaction { transaccion ->
            val cita = transaccion.get(refCita)
            val horarioNuevo = transaccion.get(refNuevo)

            // Solo se reagenda una cita propia que siga rechazada, a un horario libre
            if (!cita.exists() ||
                cita.getString("usuario_id") != usuarioId ||
                cita.getString("estado") != ESTADO_RECHAZADO ||
                horarioNuevo.exists()
            ) {
                return@runTransaction false
            }

            // Liberamos el horario anterior y apartamos el nuevo
            val anterior = idDeHorario(cita.getString("fecha_cita") ?: "", cita.getString("horario") ?: "")
            transaccion.delete(db.collection("horarios").document(anterior))
            transaccion.set(
                refNuevo,
                mapOf("fecha_cita" to fechaCita, "horario" to horario, "cita_id" to citaId)
            )

            // null = vacío: borramos quién la rechazó y por qué, porque la cita empieza de nuevo
            transaccion.update(
                refCita,
                mapOf(
                    "fecha_cita" to fechaCita,
                    "horario" to horario,
                    "estado" to ESTADO_PAGADO,
                    "atendido_por" to null,
                    "motivo_rechazo" to null
                )
            )
            true
        }.await()
    }

    // ============================================
    // CITAS (lo que usa el empleado)
    // ============================================

    // Las citas de TODOS los clientes, de la más cercana a la más lejana
    suspend fun obtenerTodasLasCitas(): List<CitaItem> {
        val resultado = db.collection("citas").get().await()
        return resultado.documents.map { comoCita(it) }.sortedBy { it.fechaCita + it.horario }
    }

    // El empleado marca una cita como completada (guarda quién y cuándo)
    suspend fun completarCita(citaId: Long, empleado: String) {
        cambiarSiEstaPagada(
            citaId,
            mapOf("estado" to ESTADO_COMPLETADO, "atendido_por" to empleado, "completado_en" to ahora())
        )
    }

    // El empleado rechaza una cita (guarda quién y por qué)
    suspend fun rechazarCita(citaId: Long, empleado: String, motivo: String) {
        cambiarSiEstaPagada(
            citaId,
            mapOf("estado" to ESTADO_RECHAZADO, "atendido_por" to empleado, "motivo_rechazo" to motivo)
        )
    }

    // Solo se cambia si la cita sigue "pagado" (por si otro empleado la atendió
    // al mismo tiempo desde la app o desde la página web)
    private suspend fun cambiarSiEstaPagada(citaId: Long, cambios: Map<String, Any?>) {
        val refCita = db.collection("citas").document(citaId.toString())
        db.runTransaction { transaccion ->
            val cita = transaccion.get(refCita)
            if (cita.exists() && cita.getString("estado") == ESTADO_PAGADO) {
                transaccion.update(refCita, cambios)
            }
        }.await()
    }

    // ============================================
    // HORARIOS
    // ============================================

    // Horarios que ya tienen cita en un día, por ejemplo ["16:00 - 16:40"].
    // Una cita rechazada sigue ocupando su horario hasta que el cliente la reagenda.
    suspend fun obtenerHorariosOcupados(fechaCita: String): List<String> {
        val resultado = db.collection("horarios").whereEqualTo("fecha_cita", fechaCita).get().await()
        return resultado.documents.mapNotNull { it.getString("horario") }
    }

    // ¿Ese horario de ese día ya tiene cita?
    suspend fun horarioOcupado(fechaCita: String, horario: String): Boolean {
        return db.collection("horarios").document(idDeHorario(fechaCita, horario)).get().await().exists()
    }

    // ============================================
    // AYUDANTES
    // ============================================

    // El id del documento que aparta un horario: "2026-11-27 15:30 - 16:10"
    private fun idDeHorario(fechaCita: String, horario: String): String = "$fechaCita $horario"

    // "2026-11-27" -> "27/11/2026" (para mostrarla en pantalla)
    fun mostrarFecha(fechaCita: String): String {
        val partes = fechaCita.split("-")
        if (partes.size != 3) return fechaCita
        return "${partes[2]}/${partes[1]}/${partes[0]}"
    }

    // Fecha y hora de este momento, igual que en la página web: "26/11/2026, 7:16:23 p.m."
    private fun ahora(): String {
        return SimpleDateFormat("dd/MM/yyyy, h:mm:ss a", Locale.forLanguageTag("es-MX")).format(Date())
    }

    // Convierte un documento de la colección "citas" en un CitaItem.
    // Si un campo no viene (o está vacío), se usa "" o 0.
    private fun comoCita(doc: DocumentSnapshot): CitaItem {
        return CitaItem(
            id = doc.id.toLongOrNull() ?: 0L,
            usuarioId = doc.getString("usuario_id") ?: "",
            cliente = doc.getString("cliente") ?: "",
            propietario = doc.getString("propietario") ?: "",
            mascota = doc.getString("mascota") ?: "",
            especie = doc.getString("especie") ?: "",
            raza = doc.getString("raza") ?: "",
            peso = doc.getDouble("peso") ?: 0.0,
            foto = doc.getString("foto") ?: "",
            servicios = doc.getString("servicios") ?: "",
            total = doc.getLong("total")?.toInt() ?: 0,
            fechaCita = doc.getString("fecha_cita") ?: "",
            horario = doc.getString("horario") ?: "",
            estado = doc.getString("estado") ?: ESTADO_PAGADO,
            atendidoPor = doc.getString("atendido_por") ?: "",
            motivoRechazo = doc.getString("motivo_rechazo") ?: "",
            completadoEn = doc.getString("completado_en") ?: ""
        )
    }
}

// Corre una consulta a Firebase sin congelar la pantalla.
// Si no hay internet (o Firebase falla), avisa con un mensaje en lugar de cerrar la app.
// Uso:  conBaseDeDatos { val citas = BaseDeDatos.obtenerCitasDeUsuario(id) ... }
fun AppCompatActivity.conBaseDeDatos(accion: suspend () -> Unit) {
    lifecycleScope.launch {
        try {
            accion()
        } catch (e: CancellationException) {
            throw e // la pantalla se cerró mientras esperaba: no es un error
        } catch (e: Exception) {
            Log.e("BaseDeDatos", "Error al hablar con Firebase", e)
            Toast.makeText(
                this@conBaseDeDatos,
                "No se pudo conectar con la base de datos. Revisa tu conexión a internet.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
