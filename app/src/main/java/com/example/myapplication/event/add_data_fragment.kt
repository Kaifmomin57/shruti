package com.example.myapplication.event

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentAddDataFragmentBinding
import com.example.myapplication.moduel.event_structure
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.storage.FirebaseStorage

class add_data_fragment : Fragment() {

  private lateinit var binding: FragmentAddDataFragmentBinding
  private var eventRef = FirebaseDatabase.getInstance().getReference("Event")
  private var selectedImageUri: Uri? = null
  private val storageRef = FirebaseStorage.getInstance().reference

  private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
    binding.imageView.setImageURI(uri)
    selectedImageUri = uri
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentAddDataFragmentBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.imageView.setOnClickListener {
      pickImage.launch("image/*")
    }

    binding.buttonAdd.setOnClickListener {
      insertData()
    }
  }

  private fun insertData() {
    val title = binding.editTextTitle.text.toString()
    val description = binding.editTextDescription.text.toString()
    val date = binding.editTextDate.text.toString()

    if (title.isBlank() || description.isBlank() || date.isBlank() || selectedImageUri == null) {
      Toast.makeText(context, "Please fill in all fields and select an image", Toast.LENGTH_SHORT).show()
      return
    }

    binding.buttonAdd.isEnabled = false
    binding.progressBar.visibility = View.VISIBLE

    val newEventRef = eventRef.push()
    val eventId = newEventRef.key
    val imageRef = storageRef.child("images/$eventId")

    val uploadTask = imageRef.putFile(selectedImageUri!!)
    uploadTask.continueWithTask { task ->
      if (!task.isSuccessful) {
        task.exception?.let { throw it }
      }
      imageRef.downloadUrl
    }.addOnCompleteListener { task ->
      if (task.isSuccessful) {
        val downloadUri = task.result
        val event = event_structure(date, description, title, downloadUri.toString(), eventId)
        newEventRef.setValue(event).addOnCompleteListener { eventTask ->
          if (eventTask.isSuccessful) {
            sendNotificationsToAll(title, description)
            navigateToEventListFragment()
            Toast.makeText(context, "Event added successfully", Toast.LENGTH_SHORT).show()
          } else {
            Toast.makeText(context, "Failed to add event", Toast.LENGTH_SHORT).show()
            resetUI()
          }
        }
      } else {
        Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
        resetUI()
      }
    }
  }

  private fun sendNotificationsToAll(eventTitle: String, eventDescription: String) {
    val userTokens = mutableListOf<String>()
    val adminTokens = mutableListOf<String>()

    // Retrieve user tokens from Realtime Database
    FirebaseDatabase.getInstance().getReference("Users").get().addOnSuccessListener { snapshot ->
      for (user in snapshot.children) {
        val token = user.child("token").getValue(String::class.java)
        token?.let { userTokens.add(it) }
      }

      // Retrieve admin tokens from Firestore
      FirebaseFirestore.getInstance().collection("Admins").get().addOnSuccessListener { docs ->
        for (doc in docs) {
          val token = doc.getString("token")
          token?.let { adminTokens.add(it) }
        }

        // Send notifications to all tokens
        val allTokens = userTokens + adminTokens
        for (token in allTokens) {
          sendNotificationToToken(token, eventTitle, eventDescription)
        }
      }
    }
  }

  private fun sendNotificationToToken(token: String, title: String, body: String) {
    FirebaseMessaging.getInstance().send(
      RemoteMessage.Builder("$token@fcm.googleapis.com")
        .setMessageId(System.currentTimeMillis().toString())
        .addData("title", title)
        .addData("body", body)
        .build()
    )
  }

  private fun resetUI() {
    binding.progressBar.visibility = View.GONE
    binding.buttonAdd.isEnabled = true
  }

  private fun navigateToEventListFragment() {
    requireActivity().supportFragmentManager.beginTransaction()
      .replace(R.id.relative, event_mam())
      .commit()
  }
}
