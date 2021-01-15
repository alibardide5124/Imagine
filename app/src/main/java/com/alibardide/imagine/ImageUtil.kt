package com.alibardide.imagine

import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Environment
import android.util.DisplayMetrics
import android.widget.ImageView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*

class ImageUtil(private val listener: ImageListener) {

    fun loadImage(context: Context, file: File, imageView: ImageView) {
        // Run an AsyncTask
        AsyncTaskNeo.executeAsyncTask<Drawable?, Boolean>(
            onPreExecute = {},
            doInBackground = {
                var drawable: Drawable? = null
                try {
                    // Get file as Bitmap
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    // Reduce size to 480
                    val sizeBase = bitmap.width / 480
                    val width = bitmap.width / sizeBase
                    val height = bitmap.height / sizeBase
                    drawable = RoundedBitmapDrawableFactory.create(context.resources,
                        Bitmap.createScaledBitmap(bitmap, width, height, true))
                    // Round the corners
                    drawable.isCircular = true
                    drawable.cornerRadius = 20f
                } catch (e: IOException) {
                    e.printStackTrace()
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
                            val root = "${getAbsoluteDir(context)}/$saveDir"
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

    private fun getAbsoluteDir(context: Context) : String {
        val rootPath = context.getExternalFilesDir(null)!!.absolutePath
        val extra = "Android/data/${BuildConfig.APPLICATION_ID}/files"
        return File(rootPath.replace(extra, "")).absolutePath
    }

}
interface ImageListener {
    fun onProgressFinish()
}