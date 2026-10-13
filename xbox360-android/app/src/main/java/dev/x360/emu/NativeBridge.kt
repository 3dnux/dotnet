package dev.x360.emu

import android.view.Surface

/** Puente JNI hacia el núcleo nativo (Xenia). Ver src/main/cpp/bridge.cpp. */
object NativeBridge {
    init { System.loadLibrary("x360core") }

    external fun coreVersion(): String
    /** Carga un título (XEX/ISO) desde un descriptor de archivo obtenido vía SAF. */
    external fun loadTitle(fd: Int): Boolean
    external fun setSurface(surface: Surface?)
    external fun start()
    external fun stop()
    /** Estado de un mando virtual: botones como bitmask XINPUT, sticks/gatillos normalizados. */
    external fun setInput(buttons: Int, lx: Float, ly: Float, rx: Float, ry: Float, lt: Float, rt: Float)
}
