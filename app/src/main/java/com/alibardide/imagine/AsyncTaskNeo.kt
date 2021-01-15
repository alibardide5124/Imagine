package com.alibardide.imagine

import kotlinx.coroutines.*

object AsyncTaskNeo {

    fun <R, P> executeAsyncTask (
        // Get functions
        onPreExecute: () -> Unit,
        doInBackground: suspend (suspend (P) -> Unit) -> R,
        onProgressUpdate: (P) -> Unit,
        onPostExecute: (R) -> Unit
    ) {
        // Run in Coroutine scope
        CoroutineScope(Dispatchers.Main).launch {
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