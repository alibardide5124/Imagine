package com.alibardide.imagine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.provider.MediaStore
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException

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
                    drawable = RoundedBitmapDrawableFactory.create(
                        context.resources,
                        Bitmap.createScaledBitmap(bitmap, width, height, true)
                    )
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

    fun saveImage(context: Context, file: File, quality: Int = 75) {
        // Run an AsyncTask
        AsyncTaskNeo.executeAsyncTask(
            onPreExecute = {},
            doInBackground = { _: suspend (progress: Int) -> Unit ->
                val imageCollection = sdk29AndUp {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI

                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "imagine.${file.name}")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.WIDTH, bitmap.width)
                    put(MediaStore.Images.Media.HEIGHT, bitmap.height)
                }
                try {
                    context.contentResolver.insert(imageCollection, contentValues)?.also { uri ->
                        context.contentResolver.openOutputStream(uri).use { outputStream ->
                            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream))
                                throw IOException("Couldn't save save bitmap")
                        }
                    } ?: throw IOException("Couldn't create MediaStore entry")
                    true
                } catch (e: IOException) {
                    e.printStackTrace()
                    false
                }
            },
            onProgressUpdate = {},
            onPostExecute = { result ->
                CoroutineScope(Dispatchers.IO).launch {
                    // Delete cached image
                    file.delete()
                    if (file.exists()) {
                        file.canonicalFile.delete()
                        if (file.exists()) context.deleteFile(file.name)
                    }
                }

                listener.onProgressFinish()
                // Display result AlertDialog
                AlertDialog.Builder(context)
                    .setTitle("Compress Picture")
                    .setMessage(
                        if (result) "File saved to: Pictures\\"
                        else "failed to compress picture!"
                    )
                    .setPositiveButton("ok", null)
                    .create()
                    .show()
            }
        )
    }

}

interface ImageListener {
    fun onProgressFinish()
}