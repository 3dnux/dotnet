package dev.x360.emu

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Dashboard inspirado en la Xbox 360: barra de "blades" arriba y, en Juegos,
 * una cuadrícula de portadas. Las demás blades son marcadores de posición.
 */
class MainActivity : AppCompatActivity() {
    private val green = Color.parseColor("#9BF00B")
    private val bg = Color.parseColor("#0B0F0A")
    private lateinit var store: LibraryStore
    private lateinit var adapter: GameAdapter
    private lateinit var content: FrameLayout
    private lateinit var gamesView: LinearLayout
    private lateinit var status: TextView

    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
        tree ?: return@registerForActivityResult
        contentResolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        status.text = "Buscando juegos…"
        Thread {
            val games = LibraryScanner.scan(this, tree)
            val merged = (store.load() + games).distinctBy { it.uri }
            store.save(merged)
            runOnUiThread { showGames(merged) }
        }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = LibraryStore(this)
        adapter = GameAdapter { game ->
            startActivity(Intent(this, EmulationActivity::class.java).setData(android.net.Uri.parse(game.uri)))
        }

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val blades = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(0, dp(24), 0, dp(12)) }
        val names = listOf("Mercado", "Juegos", "Medios", "Sistema")
        val tabs = names.map { name ->
            TextView(this).apply {
                text = name; textSize = 20f; setPadding(dp(20), dp(8), dp(20), dp(8))
                setTextColor(Color.GRAY); setOnClickListener { select(name, blades) }
            }.also { blades.addView(it) }
        }
        content = FrameLayout(this)
        status = TextView(this).apply { setTextColor(Color.LTGRAY); gravity = Gravity.CENTER; setPadding(0, dp(8), 0, dp(8)) }
        gamesView = buildGamesView()

        root.addView(blades)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        select("Juegos", blades)
        showGames(store.load())
    }

    private fun buildGamesView() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val list = RecyclerView(this@MainActivity).apply {
            layoutManager = GridLayoutManager(this@MainActivity, 4)
            adapter = this@MainActivity.adapter
            setPadding(dp(16), 0, dp(16), 0)
        }
        addView(status)
        addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        addView(TextView(this@MainActivity).apply {
            text = "＋ Añadir carpeta de juegos"; setTextColor(Color.BLACK); textSize = 16f
            gravity = Gravity.CENTER; setPadding(dp(16), dp(12), dp(16), dp(12))
            background = GradientDrawable().apply { setColor(green); cornerRadius = dp(6).toFloat() }
            setOnClickListener { folderPicker.launch(null) }
        }, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(24), dp(8), dp(24), dp(16)) })
    }

    private fun select(name: String, blades: LinearLayout) {
        for (i in 0 until blades.childCount) {
            (blades.getChildAt(i) as TextView).apply {
                val on = text == name
                setTextColor(if (on) green else Color.GRAY)
                setTypeface(null, if (on) Typeface.BOLD else Typeface.NORMAL)
            }
        }
        content.removeAllViews()
        if (name == "Juegos") content.addView(gamesView, -1, -1) else content.addView(TextView(this).apply {
            text = when (name) {
                "Sistema" -> "Núcleo: ${NativeBridge.coreVersion()}"
                else -> "Próximamente"
            }
            setTextColor(Color.LTGRAY); gravity = Gravity.CENTER
        }, -1, -1)
    }

    private fun showGames(games: List<Game>) {
        adapter.submit(games)
        status.text = if (games.isEmpty()) "No hay juegos. Añade una carpeta con tus copias (.iso, .xex, .zar)." else "${games.size} juegos"
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}

private class GameAdapter(private val onClick: (Game) -> Unit) : RecyclerView.Adapter<GameAdapter.VH>() {
    private var items: List<Game> = emptyList()
    class VH(v: View) : RecyclerView.ViewHolder(v)

    fun submit(list: List<Game>) { items = list.sortedBy { it.title.lowercase() }; notifyDataSetChanged() }
    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val d = parent.resources.displayMetrics.density
        val tile = TextView(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(-1, (150 * d).toInt()).apply { setMargins(8, 8, 8, 8) }
            gravity = Gravity.BOTTOM or Gravity.START; setPadding(24, 24, 24, 24)
            setTextColor(Color.WHITE); textSize = 14f; maxLines = 3
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#2E7D0F"), Color.parseColor("#0E2A06"))).apply { cornerRadius = 8 * d }
        }
        return VH(tile)
    }

    override fun onBindViewHolder(h: VH, i: Int) {
        val game = items[i]
        (h.itemView as TextView).text = game.title
        h.itemView.setOnClickListener { onClick(game) }
    }
}
