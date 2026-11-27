# VETSERVICE2GO (app para Android)

App para agendar citas veterinarias. Usa la misma base de datos (Firebase Cloud Firestore)
que la página web: https://aml-jordan.github.io/VETSERVICE2GO/

## Cómo abrir el proyecto

1. Instala **Android Studio 2026.1 o más nuevo** (el proyecto usa el Android Gradle Plugin 9.4.1
   y compila para Android 37).
2. Descarga el código (botón verde **Code → Download ZIP**) y descomprímelo.
3. En Android Studio: **File → Open** y elige la carpeta del proyecto (la que tiene `settings.gradle.kts`).
4. Espera a que termine el **Gradle Sync** (la primera vez descarga las librerías, necesita internet).
5. Conecta un celular o abre un emulador y presiona **Run ▶**.

No hay que configurar nada de Firebase: la conexión ya viene en `BaseDeDatos.kt`.
El celular necesita internet para usar la app.

## Cuentas para probar

| Rol | Correo | Contraseña |
|---|---|---|
| Empleado | jordan.martinez@gmail.com | 1234567 |
| Cliente | maria.garcia@gmail.com | 1234567 |

El super usuario (super.usuario@gmail.com) solo se usa en la página web.

## Archivos principales

| Archivo | Qué hace |
|---|---|
| `BaseDeDatos.kt` | Lee y guarda en Firebase (usuarios, mascotas, citas y horarios) |
| `LoginActivity.kt`, `RegisterActivity.kt` | Iniciar sesión y crear cuenta |
| `RegisterPetActivity.kt`, `FechaHoraActivity.kt`, `ResumenActivity.kt` | Agendar una cita |
| `MisCitasActivity.kt`, `PerfilActivity.kt` | Citas y perfil del cliente |
| `EmpleadoActivity.kt` | El empleado completa o rechaza citas |
