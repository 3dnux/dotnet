# X360 Emu (Android)

Frontend Android (Kotlin) + puente JNI (C++) para un núcleo de emulación de Xbox 360 basado en [Xenia](https://github.com/xenia-project/xenia).

**Estado:** esqueleto. Compila la app y el puente, pero el núcleo aún no está integrado (`loadTitle` devuelve `false`). Sin compilar todavía: el entorno de desarrollo no tenía Android SDK/NDK.

## Xenia como submódulo
`third_party/xenia` apunta a `xenia-project/xenia` (maestro, commit `95a5c3e`). Para prepararlo: `scripts/setup-xenia.sh`.

Lo que ya trae Xenia: un proyecto Android Studio propio (`third_party/xenia/android/android_studio_project`, build con premake + ndk-build para `arm64-v8a` y `x86_64`), backend de GPU Vulkan y UI de ventana. **Lo que no trae:** un backend de CPU para ARM64; en `src/xenia/cpu/backend/` solo existen `x64` y `null`. Hasta que exista un backend `a64`, en un teléfono arm64 no se ejecutará código de juegos (solo funciona en emuladores/dispositivos x86_64).

Consecuencia: el esqueleto Kotlin/CMake de `app/` es un prototipo paralelo. El camino realista es trabajar sobre el proyecto Android de Xenia y escribir el backend `a64`, no mantener dos apps.

## Hoja de ruta
1. Añadir Xenia en `third_party/xenia` (submódulo) y portar el build a CMake/NDK arm64.
2. Backend de CPU: JIT PowerPC → ARM64 (Xenia solo trae x64; es el mayor trabajo).
3. GPU: backend Vulkan de Xenia sobre `ANativeWindow`; revisar compatibilidad de drivers móviles.
4. Plataforma: archivos por SAF (fd), audio (AAudio/Oboe), entrada (mando físico + táctil → XInput).
5. UI: biblioteca de juegos, ajustes por título, compatibilidad.

## Uso legal
Solo para ejecutar copias de juegos de tu propiedad. No se incluyen juegos ni firmware de Microsoft.
