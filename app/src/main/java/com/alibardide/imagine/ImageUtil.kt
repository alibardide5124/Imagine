package com.alibardide.imagine

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Environment
import android.widget.ImageView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*

class ImageUtil(private val listener: ImageListener) {

    fun loadImage(context: Context, path: String, imageView: ImageView) {
        // Run an AsyncTask
        AsyncTaskNeo.executeAsyncTask<Drawable?, Boolean>(
            onPreExecute = {},
            doInBackground = {
                var drawable: Drawable? = null
                try {
                    // Get file from path
                    val inputStream = withContext(Dispatchers.IO) { FileInputStream(path) }
                    // Round the corners
                    drawable = RoundedBitmapDrawableFactory.create(context.resources, inputStream)
                    drawable.isCircular = true
                    drawable.cornerRadius = 20f
                } catch (e: IOException) {
                }
                drawable
            },
            onProgressUpdate = {},
            onPostExecute = { // Set image if not null
                it?.let { imageView.setImageDrawable(it) }
                listener.onProgressFinish()
            }
        )
    }

    fun saveImage(context: Context, file: File?, saveDir: String) {
        // Run an AsyncTask
        AsyncTaskNeo.executeAsyncTask(
            onPreExecute = {},
            doInBackground = { _: suspend (progress: Int) -> Unit ->
                // Check if file not null
                if (file == null) false
                else {
                    try {
                        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
                            // Get file
                            val root = context.getExternalFilesDir(null)?.absolutePath + "/$saveDir"
                            val dir = File(root)
                            if (!dir.exists()) dir.mkdirs()
                            val sFile = File(dir, "imagine.${file.name}")
                            withContext(Dispatchers.IO) {
                                FileOutputStream(sFile).use {
                                    // Save it as bitmap
                                    BitmapFactory.decodeFile(file.absolutePath)
                                        .compress(Bitmap.CompressFormat.JPEG, 70, it)
                                    it.flush()
                                }
                                true
                            }
                        } else false
                    } catch (e: IOException) {
                        false
                    }
                }
            },
            onProgressUpdate = {},
            onPostExecute = { result ->
                listener.onProgressFinish()
                // Display result AlertDialog
                val alertDialog = AlertDialog.Builder(context)
                    .setTitle("Compress Picture")
                    .setMessage("failed to compress picture!")
                    .setPositiveButton("ok", null)
                if (result) alertDialog.setMessage("File saved to:\n$saveDir")
                alertDialog.create()
                    .show()
            }
        )
    }


}
interface ImageListener {
    fun onProgressFinish()
}