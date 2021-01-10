package com.alibardide.imagine

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.resolution
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_progress.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    companion object { const val RESULT_PICK_IMAGE = 101 }
    private val saveDir = "/Pictures/Imagine/"
    private var hasImage = false
    private var imageFile: File? = null

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
        mainCompress.setOnClickListener { compress() }
        mainAbout.setOnClickListener {
            val alertDialog = AlertDialog.Builder(this)
                .setTitle("About me")
                .setMessage("Developed by Ali Bardide\n\nLicensed on Apache 2.0")
                .setPositiveButton("ok", null)
                .setNeutralButton("GitHub") { _: DialogInterface, _: Int ->
                    val url = "https://github.com/alibardide5124/imagine.git"
                    val i = Intent(Intent.ACTION_VIEW)
                    i.data = Uri.parse(url)
                    startActivity(i)
                }
            alertDialog.create()
                .show()
        }
    }
    private fun pickImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, RESULT_PICK_IMAGE)
    }
    private fun compress() {
        if (hasImage) {
            imageFile?.let { file ->
                val dialog = progressAlertDialog()
                dialog.show()
                GlobalScope.launch {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    val compressedImage = Compressor.compress(this@MainActivity, file) {
                        resolution(bitmap.width, bitmap.height)
                    }
                    savePic(compressedImage, dialog)
                }
            }
        } else { toast("No image selected")}
    }
    private fun toast(message: String, length: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, length).show()
    }
    private fun loadImage(path: String, imageView: ImageView) {
        AsyncTaskNeo(this).executeAsyncTask<Drawable?, Boolean> (
            onPreExecute = {},
            doInBackground = {
                var drawable: Drawable? = null
                try {
                    val inputStream = FileInputStream(path)
                    drawable = RoundedBitmapDrawableFactory.create(resources, inputStream)
                    drawable.isCircular = true
                    drawable.cornerRadius = 20f
                } catch (e: IOException) {}
                drawable
            },
            onProgressUpdate = {},
            onPostExecute = {
                it?.let { imageView.setImageDrawable(it) }
            }
        )
    }
    private fun savePic(file: File?, dialog: AlertDialog) {
        AsyncTaskNeo(this).executeAsyncTask (
            onPreExecute = {},
            doInBackground = { _: suspend (progress: Int) -> Unit ->
                if (file == null) false
                else {
                    try {
                        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
                            val root =
                                Environment.getExternalStorageDirectory().absolutePath + saveDir
                            val dir = File(root)
                            if (!dir.exists()) dir.mkdirs()
                            val sFile = File(dir, "imagine.${file.name}")
                            FileOutputStream(sFile).use {
                                BitmapFactory.decodeFile(file.absolutePath)
                                    .compress(Bitmap.CompressFormat.JPEG, 70, it)
                                it.flush()
                            }
                            true
                        } else { false }
                    } catch (e: IOException) { false }
                }
            },
            onProgressUpdate = {},
            onPostExecute = { result ->
                dialog.dismiss()
                val alertDialog = AlertDialog.Builder(this)
                    .setTitle("Compress Picture")
                    .setMessage("failed to compress picture!")
                    .setPositiveButton("ok", null)
                if (result) alertDialog.setMessage("File saved to:\n$saveDir")
                alertDialog.create()
                    .show()
            }
        )
    }
    private fun progressAlertDialog() : AlertDialog {
        val v = layoutInflater.inflate(R.layout.dialog_progress, null, false)
        v.dialogProgressMessage.text = getString(R.string.wait_moment)
        return AlertDialog.Builder(this)
            .setView(v)
            .setCancelable(false)
            .create()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val dialog = progressAlertDialog()
        if (requestCode == RESULT_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                toast("Failed to open picture!")
                return
            }
            try {
                dialog.show()
                imageFile = FileUtil.from(this, data.data!!).also {
                    loadImage(it.path, mainPicture)
                    hasImage = true
                    mainCompress.show()
                    dialog.dismiss()
                }
            } catch (e: IOException) {
                toast("Failed to read picture data!")
                e.printStackTrace()
            }
        }
    }
}