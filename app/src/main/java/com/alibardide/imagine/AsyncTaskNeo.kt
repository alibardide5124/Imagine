package com.alibardide.imagine

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class AsyncTaskNeo(private val context: Context) {

    fun <R, P> executeAsyncTask (
        // Get functions
        onPreExecute: () -> Unit,
        doInBackground: suspend (suspend (P) -> Unit) -> R,
        onProgressUpdate: (P) -> Unit,
        onPostExecute: (R) -> Unit
    ) {
        // Run in context lifecycle
        (context as AppCompatActivity).lifecycleScope.launch {
            // Run PreExecute
            onPreExecute()
            // Then run doInBackground on IO thread and pass data to result
            val result = withContext(Dispatchers.IO) {
                doInBackground {
                    // Run onProgress update in main thread
                    withContext(Dispatchers.Main) { onProgressUpdate(it) }
                }
            }
            // On the last run onPostExecute
            onPostExecute(result)
        }
    }


}