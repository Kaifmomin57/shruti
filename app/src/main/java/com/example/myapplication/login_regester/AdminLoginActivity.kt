package com.example.myapplication.login_regester

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import com.google.firebase.firestore.FirebaseFirestore
import android.text.InputType
import android.view.MotionEvent
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.example.myapplication.man_side.MamSideActivity

class AdminLoginActivity : AppCompatActivity() {

  private lateinit var etPasscode: EditText
  private lateinit var btnVerifyPasscode: Button
  private lateinit var ivTogglePasscode: ImageView

  private val db = FirebaseFirestore.getInstance()

  @SuppressLint("ClickableViewAccessibility")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Check if the admin is already logged in
    val preferences = getSharedPreferences("AdminPrefs", MODE_PRIVATE)
    val isLoggedIn = preferences.getBoolean("isLoggedIn", false)

    if (isLoggedIn) {
      // If already logged in, skip login and go directly to dashboard
      val intent = Intent(this, MamSideActivity::class.java)
      startActivity(intent)
      finish()
      return // Important to prevent the rest of onCreate from running
    }


    setContentView(R.layout.activity_admin_login)

    etPasscode = findViewById(R.id.etPasscode)
    btnVerifyPasscode = findViewById(R.id.btnVerifyPasscode)
    ivTogglePasscode = findViewById(R.id.ivTogglePasscode)

    etPasscode.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

    ivTogglePasscode.setOnTouchListener { _, event ->
      when (event.action) {
        MotionEvent.ACTION_DOWN -> {
          // Show password
          etPasscode.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
          ivTogglePasscode.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.baseline_eye_24))
        }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
          // Hide password
          etPasscode.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
          ivTogglePasscode.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.baseline_eye_24))
        }
      }
      // Move cursor to the end of the text
      etPasscode.setSelection(etPasscode.text.length)
      true // Consume the touch event
    }

    btnVerifyPasscode.setOnClickListener {
      val enteredPasscode = etPasscode.text.toString().trim()
      if (enteredPasscode.isNotEmpty()) {
        verifyPasscode(enteredPasscode)
      } else {
        Toast.makeText(this, "Please enter the passcode", Toast.LENGTH_SHORT).show()
      }
    }
  }

  private fun verifyPasscode(enteredPasscode: String) {
    db.collection("admin").document("credentials")
      .get()
      .addOnSuccessListener { adminDoc ->
        val adminPasscode = adminDoc.getString("passcode")
        if (enteredPasscode == adminPasscode) {
          proceedToDashboard(false) // Admin login
          return@addOnSuccessListener
        }

        db.collection("super_admin").document("config")
          .get()
          .addOnSuccessListener { superAdminDoc ->
            val superAdminPasscode = superAdminDoc.getString("password")
            if (enteredPasscode == superAdminPasscode) {
              proceedToDashboard(true) // Super admin login
              return@addOnSuccessListener
            }

            Toast.makeText(this, "Incorrect passcode", Toast.LENGTH_SHORT).show()
          }
          .addOnFailureListener {
            Toast.makeText(this, "Failed to fetch super admin passcode", Toast.LENGTH_SHORT).show()
          }
      }
      .addOnFailureListener {
        Toast.makeText(this, "Failed to fetch admin passcode", Toast.LENGTH_SHORT).show()
      }
  }

  private fun proceedToDashboard(isSuperAdmin: Boolean) {
    val preferences = getSharedPreferences("AdminPrefs", MODE_PRIVATE)
    val editor = preferences.edit()
    editor.putBoolean("isLoggedIn", true)
    editor.putBoolean("isSuperAdmin", isSuperAdmin)
    editor.apply()

    val intent = Intent(this, MamSideActivity::class.java)
    startActivity(intent)
    finish()
  }
}