package com.example.myapplication.man_side

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.Support
import com.example.myapplication.contact
import com.example.myapplication.databinding.ActivityMamSideBinding
import com.example.myapplication.event.event_mam
import com.example.myapplication.login_regester.AdminLoginActivity
import com.example.myapplication.login_regester.RegisterActivity
import com.example.myapplication.login_regester.ResetPasswordActivity
import com.example.myapplication.volunter.volunter
import nav_fregment.aboutus
import nav_fregment.home
import nav_fregment.profile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MamSideActivity : AppCompatActivity() {
  private lateinit var binding: ActivityMamSideBinding
  private val excelFileName = "preasenty.xlsx"
  private val PERMISSION_REQUEST_CODE = 100

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMamSideBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setupNavigation()
    checkPermissionsAndDownloadExcelFile()
  }

  private fun setupNavigation() {
    intent.getStringExtra("fragment_to_show")?.let {
      if (it == "event_mam") replaceFragment(event_mam())
    } ?: replaceFragment(home())

    binding.bottnavigation.setOnItemSelectedListener {
      when (it.itemId) {
        R.id.about -> replaceFragment(aboutus())
        R.id.volunter -> replaceFragment(volunter())
        R.id.home -> replaceFragment(home())
        R.id.events -> replaceFragment(event_mam())
        R.id.profile -> replaceFragment(profile())
      }
      true
    }

    binding.toolbar.setOnClickListener {
      if (binding.drawerLayout.isDrawerOpen(binding.sidemenu)) {
        binding.drawerLayout.closeDrawer(binding.sidemenu)
      } else {
        binding.drawerLayout.openDrawer(binding.sidemenu)
      }
    }

    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(binding.sidemenu)) {
          binding.drawerLayout.closeDrawer(binding.sidemenu)
        } else {
          isEnabled = false
          onBackPressedDispatcher.onBackPressed()
        }
      }
    })

    binding.sidemenu.setNavigationItemSelectedListener {
      when (it.itemId) {
        R.id.nav_contact -> startActivity(Intent(this, contact::class.java))
        R.id.nav_support -> startActivity(Intent(this, Support::class.java))
        R.id.nav_logout -> logoutAdmin()
        R.id.regester -> startActivity(Intent(this, RegisterActivity::class.java))
        R.id.ForgetPassword -> startActivity(Intent(this, ResetPasswordActivity::class.java))
        R.id.nav_presenty -> openLocalExcelFile()
      }
      true
    }
  }

  private fun replaceFragment(fragment: Fragment) {
    supportFragmentManager.beginTransaction()
      .replace(binding.relative.id, fragment)
      .commit()
  }

  private fun logoutAdmin() {
    getSharedPreferences("AdminPrefs", MODE_PRIVATE).edit().apply {
      putBoolean("isLoggedIn", false)
      putBoolean("isSuperAdmin", false)
      apply()
    }
    startActivity(Intent(this, AdminLoginActivity::class.java))
    finish()
  }

  private fun checkPermissionsAndDownloadExcelFile() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      // Scoped storage on Android 11+ does not require explicit permissions for app-specific storage
      downloadExcelFileOnInstall()
    } else {
      // For Android 10 and below, check READ_EXTERNAL_STORAGE permission
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
        downloadExcelFileOnInstall()
      } else {
        ActivityCompat.requestPermissions(
          this,
          arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
          PERMISSION_REQUEST_CODE
        )
      }
    }
  }

  private fun downloadExcelFileOnInstall() {
    val sharedPrefs = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    if (sharedPrefs.getBoolean("isFirstLaunch", true)) {
      copyExcelFileToAppSpecificDir()
      sharedPrefs.edit().putBoolean("isFirstLaunch", false).apply()
    }
  }

  private fun copyExcelFileToAppSpecificDir() {
    val file = File(getExternalFilesDir(null), excelFileName)
    if (file.exists()) {
      Toast.makeText(this, "Excel file already exists.", Toast.LENGTH_SHORT).show()
      return
    }

    try {
      assets.open(excelFileName).use { inputStream ->
        FileOutputStream(file).use { outputStream ->
          inputStream.copyTo(outputStream)
        }
      }
      Toast.makeText(this, "Excel file copied to app-specific storage.", Toast.LENGTH_SHORT).show()
    } catch (e: IOException) {
      Toast.makeText(this, "Failed to copy Excel file: ${e.message}", Toast.LENGTH_SHORT).show()
      Log.e("MamSideActivity", "Error copying file: ${e.message}")
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      downloadExcelFileOnInstall()
    } else {
      showPermissionDeniedDialog()
    }
  }

  private fun showPermissionDeniedDialog() {
    AlertDialog.Builder(this)
      .setTitle("Permission Denied")
      .setMessage("This app needs storage permission to access files. Please enable it in Settings.")
      .setPositiveButton("Go to Settings") { _, _ ->
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        startActivity(intent)
      }
      .setNegativeButton("Cancel", null)
      .show()
  }

  private fun openLocalExcelFile() {
    val localFile = File(getExternalFilesDir(null), excelFileName)
    if (localFile.exists()) {
      val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", localFile)
      val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      try {
        startActivity(intent)
      } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "No app found to open Excel files", Toast.LENGTH_SHORT).show()
      }
    } else {
      Toast.makeText(this, "Excel file not found.", Toast.LENGTH_SHORT).show()
    }
  }
}
