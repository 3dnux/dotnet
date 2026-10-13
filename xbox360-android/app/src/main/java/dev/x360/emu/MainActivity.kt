package dev.x360.emu

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/** Pantalla inicial: elegir un título propio (XEX/ISO) mediante el selector de archivos del sistema. */
class MainActivity : AppCompatActivity() {
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent(this, EmulationActivity::class.java).setData(uri))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 96, 48, 48) }
        root.addView(TextView(this).apply { text = "Núcleo: ${NativeBridge.coreVersion()}" })
        root.addView(Button(this).apply {
            text = "Abrir juego (XEX / ISO)"
            setOnClickListener { picker.launch(arrayOf("*/*")) }
        })
        setContentView(root)
    }
}
