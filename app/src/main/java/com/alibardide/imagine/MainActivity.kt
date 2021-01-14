package com.alibardide.imagine

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.resolution
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_progress.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {

    companion object { const val RESULT_PICK_IMAGE = 101 }
    private val saveDir = "Pictures/Imagine/"
    private var hasImage = false
    private var imageFile: File? = null
    private lateinit var progress: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progress = progressAlertDialog()
        mainPick.setOnClickListener { pickImage() }
        mainCompress.setOnClickListener { compress() }
        mainAbout.setOnClickListener { aboutDialog().show() }
    }
    private fun pickImage() {
        // Check if we have permissions
        val listener = object: PermissionListener {
            override fun onPermissionGranted() {
                // Pick image file
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                startActivityForResult(intent, RESULT_PICK_IMAGE)
            }
            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                toast("Permission Denied").show()
            }
        }
        // Get Permission with TedPermission library
        TedPermission.with(this)
            .setPermissionListener(listener)
            .setPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .check()
    }
    private fun compress() {
        // Check if we have any image
        if (hasImage) {
            // If image not null do something
            imageFile?.let { file ->
                progress.show()
                // Launch a GlobalScope
                GlobalScope.launch {
                    // Decode selected file, compress file and save compressed file
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    val compressedImage = Compressor.compress(this@MainActivity, file) {
                        resolution(bitmap.width, bitmap.height)
                    }
                    ImageUtil.save(this@MainActivity, compressedImage, saveDir)
                    progress.dismiss()
                }
            }
        } else { toast("No image selected").show() }
    }
    private fun aboutDialog() : AlertDialog {
        // About alert dialog
        // info for get app version
        val info = packageManager.getPackageInfo(packageName, 0)
        return AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_name) + " ${info.versionName}")
            .setMessage("Developed by Ali Bardide\n\nLicensed on Apache 2.0")
            .setPositiveButton("ok", null)
            .setNeutralButton("GitHub") { _: DialogInterface, _: Int ->
                // Open project github page
                val url = "https://github.com/alibardide5124/imagine.git"
                val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(i)
            }.create()
    }
    private fun progressAlertDialog() : AlertDialog {
        // Return an AlertDialog but in ProgressDialog shape
        val v = layoutInflater.inflate(R.layout.dialog_progress, null, false)
        v.dialogProgressMessage.text = getString(R.string.wait_moment)
        return AlertDialog.Builder(this)
            .setView(v)
            .setCancelable(false)
            .create()
    }
    private fun toast(message: String, length: Int = Toast.LENGTH_SHORT) : Toast {
        return Toast.makeText(this, message, length)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            // Check if data not null
            if (data == null) {
                toast("Failed to open picture!").show()
                return
            }
            try {
                progress.show()
                // Load image into a file
                imageFile = FileUtil.from(this, data.data!!).also {
                    ImageUtil.load(this, it.path, mainPicture)
                    hasImage = true
                    // Make mainCompress button visible
                    mainCompress.show()
                    progress.dismiss()
                }
            } catch (e: IOException) { toast("Failed to read picture data!").show() }
        }
    }
}