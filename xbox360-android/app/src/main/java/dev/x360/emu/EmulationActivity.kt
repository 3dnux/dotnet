package dev.x360.emu

import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity

class EmulationActivity : AppCompatActivity(), SurfaceHolder.Callback {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = SurfaceView(this).also { it.holder.addCallback(this) }
        setContentView(view)
        val uri = intent.data ?: return finish()
        val pfd = contentResolver.openFileDescriptor(uri, "r") ?: return finish()
        if (NativeBridge.loadTitle(pfd.detachFd())) NativeBridge.start() else finish()
    }

    override fun surfaceCreated(h: SurfaceHolder) = NativeBridge.setSurface(h.surface)
    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hh: Int) = Unit
    override fun surfaceDestroyed(h: SurfaceHolder) = NativeBridge.setSurface(null)
    override fun onDestroy() { NativeBridge.stop(); super.onDestroy() }
}
