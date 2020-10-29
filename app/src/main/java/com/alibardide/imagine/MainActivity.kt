package com.alibardide.imagine

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.BitmapCompat
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.lifecycle.lifecycleScope
import com.alibardide.booklet.utils.FileUtil
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import id.zelory.compressor.loadBitmap
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val RESULT_PICK_IMAGE = 101
    private var hasImage = false
    private var imageFile: File? = null
    private var compressedImage: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainPick.setOnClickListener {
            val listener = object: PermissionListener {
                override fun onPermissionGranted() {
                    pickImage()
                }

                override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                    toast("Permission Denied")
                }

            }
            TedPermission.with(this)
                .setPermissionListener(listener)
                .setPermissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .check()
        }
        mainCompress.setOnClickListener {
            compress()
        }
    }
    private fun pickImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, RESULT_PICK_IMAGE)
    }
    private fun compress() {
        GlobalScope.launch {
            if (hasImage && imageFile != null) {
                val bitmap = BitmapFactory.decodeFile(imageFile?.absolutePath)
                compressedImage = Compressor.compress(this@MainActivity, imageFile!!) {
                    resolution(bitmap.width, bitmap.height)
                }
            }
        } .also {
            if (hasImage && compressedImage != null)
                SavePic(this@MainActivity, compressedImage).execute()
        }
    }
    private fun toast(message: String, length: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, length).show()
    }

    class LoadImage(
        private val context: Context,
        private val path: String,
        private val imageView: ImageView
    ) : AsyncTask<Boolean, Void, Drawable?>() {

        override fun doInBackground(vararg p0: Boolean?): Drawable? {
            var drawable: Drawable? = null
            try {
                val inputStream = FileInputStream(path)
                drawable = RoundedBitmapDrawableFactory.create(context.resources, inputStream)
                drawable.isCircular = true
                drawable.cornerRadius = 20f
            } catch (e: IOException) {
                Log.e("Bitmap", e.toString())
            }
            return drawable
        }

        override fun onPostExecute(result: Drawable?) {
            if (result != null) imageView.setImageDrawable(result)
        }
    }
    class SavePic(
        private val context: Context,
        private val file: File?
    ) : AsyncTask<Boolean, Void, Boolean>() {
        private val progress = ProgressDialog(context)

        override fun onPreExecute() {
            progress.setMessage("Wait a moment...")
            progress.setCancelable(false)
            progress.show()
        }
        override fun doInBackground(vararg p0: Boolean?): Boolean {
            if (file == null) return false
            try {
                if (Environment.MEDIA_MOUNTED != Environment.getExternalStorageState()) return false
                val root = Environment.getExternalStorageDirectory().absolutePath + "/Pictures/Imagine"
                val dir = File(root)
                if (!dir.exists()) dir.mkdirs()
                val sFile = File(dir, "imagine.${file.name}")
                FileOutputStream(sFile).use {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 69, it)
                    it.flush()
                }
                return true
            } catch (e: IOException) {
                Log.e("BitmapCompress", "$e")
                return false
            }
        }

        override fun onPostExecute(result: Boolean) {
            super.onPostExecute(result)
            progress.dismiss()
            val alertDialog = AlertDialog.Builder(context)
                .setTitle("Compress Picture")
                .setMessage("failed to compress picture!")
                .setPositiveButton("ok", null)
            if (result) {
                alertDialog.setMessage("File saved to:\nPictures/Imagine/")
            }
            alertDialog.create()
                .show()
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RESULT_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                toast("Failed to open picture!")
                return
            }
            try {
                val progressDialog = ProgressDialog(this)
                progressDialog.setMessage("Wait a moment..")
                progressDialog.setCancelable(false)
                progressDialog.show()
                imageFile = FileUtil.from(this, data.data!!).also {
                    LoadImage(this, it.path, mainPicture).execute()
                    hasImage = true
                    mainCompress.visibility = View.VISIBLE
                    progressDialog.dismiss()
                }
            } catch (e: IOException) {
                toast("Failed to read picture data!")
                e.printStackTrace()
            }
        }
    }
}