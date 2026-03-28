package nostalgia.memoir.screens.data

import android.content.Context

fun loadJournalEntry(
    context: Context,
    assetPath: String,
    defaultValue: String,
): String = BackendUiBridge.loadJournalEntry(context, assetPath, defaultValue)

fun saveJournalEntry(context: Context, assetPath: String, text: String) {
    BackendUiBridge.saveJournalEntry(context, assetPath, text)
}
