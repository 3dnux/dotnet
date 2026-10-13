package dev.x360.emu

import android.view.Surface

/** Puente JNI hacia el núcleo nativo (Xenia). Ver src/main/cpp/bridge.cpp. */
object NativeBridge {
    init { System.loadLibrary("x360core") }

    external fun coreVersion(): String

    private external fun readXexMetaNative(fd: Int): Array<ByteArray>?

    /** Título, ID e icono de un XEX; null si el archivo no es un XEX2 válido. */
    fun readXexMeta(fd: Int): XexMeta? {
        val (title, icon, id) = readXexMetaNative(fd) ?: return null
        val titleId = id.fold(0) { acc, b -> (acc shl 8) or (b.toInt() and 0xFF) }
        return XexMeta(titleId, String(title, Charsets.UTF_8), icon.takeIf { it.isNotEmpty() })
    }
    /** Carga un título (XEX/ISO) desde un descriptor de archivo obtenido vía SAF. */
    external fun loadTitle(fd: Int): Boolean
    external fun setSurface(surface: Surface?)
    external fun start()
    external fun stop()
    /** Estado de un mando virtual: botones como bitmask XINPUT, sticks/gatillos normalizados. */
    external fun setInput(buttons: Int, lx: Float, ly: Float, rx: Float, ry: Float, lt: Float, rt: Float)
}

class XexMeta(val titleId: Int, val title: String, val icon: ByteArray?)
