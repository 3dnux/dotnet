# X360 Emu (Android)

Frontend Android (Kotlin) + puente JNI (C++) para un núcleo de emulación de Xbox 360 basado en [Xenia](https://github.com/xenia-project/xenia).

**Estado:** esqueleto. Compila la app y el puente, pero el núcleo aún no está integrado (`loadTitle` devuelve `false`). Sin compilar todavía: el entorno de desarrollo no tenía Android SDK/NDK.

## Hoja de ruta
1. Añadir Xenia en `third_party/xenia` (submódulo) y portar el build a CMake/NDK arm64.
2. Backend de CPU: JIT PowerPC → ARM64 (Xenia solo trae x64; es el mayor trabajo).
3. GPU: backend Vulkan de Xenia sobre `ANativeWindow`; revisar compatibilidad de drivers móviles.
4. Plataforma: archivos por SAF (fd), audio (AAudio/Oboe), entrada (mando físico + táctil → XInput).
5. UI: biblioteca de juegos, ajustes por título, compatibilidad.

## Uso legal
Solo para ejecutar copias de juegos de tu propiedad. No se incluyen juegos ni firmware de Microsoft.
