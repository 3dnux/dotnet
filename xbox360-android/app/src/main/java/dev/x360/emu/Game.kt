package dev.x360.emu

/** Un título de la biblioteca. [titleId] e [iconPath] solo existen si se leyeron del XEX. */
data class Game(
    val uri: String,
    val title: String,
    val titleId: String? = null,
    val iconPath: String? = null,
)
