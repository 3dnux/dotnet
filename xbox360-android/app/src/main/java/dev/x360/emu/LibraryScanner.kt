package dev.x360.emu

import android.content.Context
import android.net.Uri
import java.io.File
import android.provider.DocumentsContract

/** Recorre una carpeta elegida con el selector del sistema (SAF) y devuelve los títulos encontrados. */
object LibraryScanner {
    private val EXTENSIONS = setOf("iso", "xex", "zar")

    fun scan(context: Context, tree: Uri): List<Game> {
        val found = mutableListOf<Game>()
        fun walk(parentDocId: String) {
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentDocId)
            val dirs = mutableListOf<String>()
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            )
            context.contentResolver.query(children, projection, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0)
                    val name = c.getString(1) ?: continue
                    if (c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                        dirs += id
                    } else if (name.substringAfterLast('.', "").lowercase() in EXTENSIONS) {
                        val uri = DocumentsContract.buildDocumentUriUsingTree(tree, id)
                        val fallback = name.substringBeforeLast('.')
                        found += if (name.endsWith(".xex", ignoreCase = true)) fromXex(context, uri, fallback)
                                 else Game(uri.toString(), fallback)
                    }
                }
            }
            dirs.forEach(::walk)
        }
        walk(DocumentsContract.getTreeDocumentId(tree))
        return found
    }

    /** Lee título, ID e icono del XEX; si falla, usa el nombre del archivo. */
    private fun fromXex(context: Context, uri: Uri, fallbackTitle: String): Game {
        val meta = try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { NativeBridge.readXexMeta(it.fd) }
        } catch (e: Exception) { null }
        if (meta == null) return Game(uri.toString(), fallbackTitle)
        val id = "%08X".format(meta.titleId)
        var iconPath: String? = null
        meta.icon?.let {
            val dir = File(context.filesDir, "icons").apply { mkdirs() }
            File(dir, "$id.png").also { f -> f.writeBytes(it); iconPath = f.absolutePath }
        }
        return Game(uri.toString(), meta.title.ifBlank { fallbackTitle }, id, iconPath)
    }
}
