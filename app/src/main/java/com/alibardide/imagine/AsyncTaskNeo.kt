package com.alibardide.imagine

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class AsyncTaskNeo(private val context: Context) {

    fun <R, P> executeAsyncTask (
        onPreExecute: () -> Unit,
        doInBackground: suspend (suspend (P) -> Unit) -> R,
        onProgressUpdate: (P) -> Unit,
        onPostExecute: (R) -> Unit
    ) {
        (context as AppCompatActivity).lifecycleScope.launch {
            onPreExecute()
            val result = withContext(Dispatchers.IO) {
                doInBackground {
                    withContext(Dispatchers.Main) { onProgressUpdate(it) }
                }
            }
            onPostExecute(result)
        }
    }


}