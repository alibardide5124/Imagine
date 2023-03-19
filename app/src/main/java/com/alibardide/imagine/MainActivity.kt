package com.alibardide.imagine

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.alibardide.imagine.databinding.ActivityMainBinding
import com.alibardide.imagine.databinding.DialogProgressBinding
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.resolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks, ImageListener {

    companion object {
        const val PERMISSION_READ_EXTERNAL_STORAGE_REQUEST_CODE = 101
        const val PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 102
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var progress: AlertDialog
    private var imageFile: File? = null
    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            onActivityResult(result)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progress = progressAlertDialog()
        binding.mainAbout.setOnClickListener { aboutDialog().show() }

        binding.mainPick.setOnClickListener {
            if (hasReadExternalStoragePermission())
                choosePicture()
            else
                requestReadExternalStoragePermission()
        }
        binding.mainCompress.setOnClickListener {
            if (hasWriteExternalStoragePermission())
                compressPicture()
            else
                requestWriteExternalStoragePermission()
        }
    }

    private fun choosePicture() {
        // Pick image file
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        resultLauncher.launch(intent)
    }

    private fun compressPicture() {
        // Check if image is not null
        imageFile?.let { file ->
            progress.show()
            lifecycleScope.launch(Dispatchers.IO) {
                val context = this@MainActivity
                // Decode selected file, compress file and save compressed file
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                val compressedImage = Compressor.compress(context, file) {
                    resolution(bitmap.width, bitmap.height)
                }
                withContext(Dispatchers.Main) {
                    // Save image to external storage
                    ImageUtil(context).saveImage(context, compressedImage)
                }
            }
        }
    }

    private fun aboutDialog(): AlertDialog {
        // About alert dialog
        // info for get app version
        return AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_name) + " ${BuildConfig.VERSION_NAME}")
            .setMessage("Developed by Ali Bardide\nLicensed on Apache 2.0")
            .setPositiveButton("ok", null)
            .setNeutralButton("GitHub") { _: DialogInterface, _: Int ->
                // Open project github page
                val url = "https://github.com/alibardide5124/imagine.git"
                val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(i)
            }.create()
    }

    private fun progressAlertDialog(): AlertDialog {
        // Return an AlertDialog but in ProgressDialog shape
        val binding = DialogProgressBinding.inflate(layoutInflater)
        binding.dialogProgressMessage.text = getString(R.string.wait_moment)
        return AlertDialog.Builder(this)
            .setView(binding.root)
            .setCancelable(false)
            .create()
    }

    private fun onActivityResult(result: ActivityResult) {
        // Check if operation was successful
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                progress.show()
                // Load image into a file
                lifecycleScope.launch(Dispatchers.IO) {
                    val context = this@MainActivity
                    imageFile = FileUtil.from(context, result.data?.data!!).also {
                        ImageUtil(context).loadImage(context, it, binding.mainPicture)
                        withContext(Dispatchers.Main) { binding.mainCompress.show() }
                    }
                }
            } catch (e: IOException) {
                Toast.makeText(this, "Failed to open picture!", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    private fun hasReadExternalStoragePermission() =
        EasyPermissions.hasPermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE)

    private fun hasWriteExternalStoragePermission() =
        EasyPermissions.hasPermissions(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private fun requestReadExternalStoragePermission() {
        EasyPermissions.requestPermissions(
            this,
            "I need your read storage permission to choose picture",
            PERMISSION_READ_EXTERNAL_STORAGE_REQUEST_CODE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    private fun requestWriteExternalStoragePermission() {
        EasyPermissions.requestPermissions(
            this,
            "I need your write storage permission to save picture",
            PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        when {
            EasyPermissions.somePermissionPermanentlyDenied(
                this,
                perms
            ) -> SettingsDialog.Builder(
                this
            ).build().show()
            requestCode == PERMISSION_READ_EXTERNAL_STORAGE_REQUEST_CODE -> requestReadExternalStoragePermission()
            requestCode == PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE -> requestWriteExternalStoragePermission()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        if (requestCode == PERMISSION_READ_EXTERNAL_STORAGE_REQUEST_CODE)
            choosePicture()
        else if (requestCode == PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE)
            compressPicture()
    }

    override fun onProgressFinish() =
        progress.dismiss()

}