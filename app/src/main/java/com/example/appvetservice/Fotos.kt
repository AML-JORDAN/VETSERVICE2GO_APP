package com.example.appvetservice

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlin.math.max

// Fotos de las mascotas.
// 1. Al elegirla, guardamos una copia pequeña dentro de la app (la galería solo nos
//    "presta" la foto mientras la pantalla está abierta).
// 2. Al guardar la cita, esa copia se convierte en texto ("data:image/jpeg;base64,...")
//    y se guarda en Firebase, igual que hace la página web. Así la foto se ve
//    en la app y en la página.
object Fotos {

    // Lado más largo de la foto, en pixeles. Pequeña para que pese poco en Firebase.
    private const val LADO_MAXIMO = 400

    // Copia la foto elegida dentro de la app. Regresa la dirección de la copia (o null si falla).
    fun copiarReducida(context: Context, uri: Uri): Uri? {
        return try {
            val lector = context.contentResolver

            // 1. Medir la foto sin cargarla completa
            val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            lector.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, medidas) }
            if (medidas.outWidth <= 0 || medidas.outHeight <= 0) return null

            // 2. Cargarla a la mitad, a la cuarta parte... (gasta menos memoria)
            var escala = 1
            while (max(medidas.outWidth, medidas.outHeight) / (escala * 2) >= LADO_MAXIMO) {
                escala *= 2
            }
            val opciones = BitmapFactory.Options().apply { inSampleSize = escala }
            var foto = lector.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
                ?: return null

            // 3. Dejarla exactamente de LADO_MAXIMO, sin deformarla
            val ladoMayor = max(foto.width, foto.height)
            if (ladoMayor > LADO_MAXIMO) {
                val proporcion = LADO_MAXIMO.toFloat() / ladoMayor
                foto = foto.scale((foto.width * proporcion).toInt(), (foto.height * proporcion).toInt())
            }

            // 4. Girarla si la cámara la guardó "acostada" (eso viene en los datos EXIF)
            val grados = lector.openInputStream(uri)?.use { gradosDeGiro(ExifInterface(it)) } ?: 0
            if (grados != 0) {
                val giro = Matrix().apply { postRotate(grados.toFloat()) }
                foto = Bitmap.createBitmap(foto, 0, 0, foto.width, foto.height, giro, true)
            }

            // 5. Guardarla como JPG dentro de la app
            val archivo = File(context.filesDir, "mascota_${System.currentTimeMillis()}.jpg")
            archivo.outputStream().use { foto.compress(Bitmap.CompressFormat.JPEG, 75, it) }
            archivo.toUri()
        } catch (_: Exception) {
            null
        }
    }

    // La copia -> texto "data:image/jpeg;base64,..." para guardarla en Firebase.
    // Regresa "" si no hay foto.
    fun comoTexto(context: Context, uri: String?): String {
        if (uri.isNullOrBlank()) return ""
        return try {
            val bytes = context.contentResolver.openInputStream(uri.toUri())?.use { it.readBytes() } ?: return ""
            "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Exception) {
            ""
        }
    }

    // Texto "data:image/jpeg;base64,..." (guardado en Firebase) -> imagen para mostrarla.
    // Regresa null si no hay foto o no se puede leer.
    fun comoImagen(texto: String): Bitmap? {
        if (texto.isBlank()) return null
        return try {
            // Lo que va después de la coma es la foto
            val bytes = Base64.decode(texto.substringAfter(","), Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    // Borra las copias que quedaron dentro de la app: una vez guardada la cita,
    // la foto ya vive en Firebase. También borra las de citas que no se terminaron.
    fun borrarCopias(context: Context) {
        val archivos = context.filesDir.listFiles { archivo -> archivo.name.startsWith("mascota_") } ?: return
        for (archivo in archivos) {
            archivo.delete()
        }
    }

    private fun gradosDeGiro(exif: ExifInterface): Int {
        return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }
}
