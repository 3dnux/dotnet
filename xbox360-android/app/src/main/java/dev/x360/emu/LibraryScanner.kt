package dev.x360.emu

import android.content.Context
import android.net.Uri
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
                        found += Game(uri.toString(), name.substringBeforeLast('.'))
                    }
                }
            }
            dirs.forEach(::walk)
        }
        walk(DocumentsContract.getTreeDocumentId(tree))
        return found
    }
}
