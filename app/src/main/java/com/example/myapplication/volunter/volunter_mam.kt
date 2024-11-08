package com.example.myapplication.volunter

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.adapter.volunter_adapter
import com.example.myapplication.databinding.ErrorDialogBinding
import com.example.myapplication.databinding.FragmentVolunterBinding
import com.example.myapplication.man_side.MamSideActivity
import com.example.myapplication.moduel.volunter_viewmodel
import com.example.myapplication.repo.volunter_repo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import volunter_viewmodelFactory

class volunter : Fragment() {

    private lateinit var binding: FragmentVolunterBinding
    private lateinit var pickExcelFileLauncher: ActivityResultLauncher<Intent>
    private var adapter: volunter_adapter? = null
    private var excelFileUri: Uri? = null
    private val repository = volunter_repo()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVolunterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: volunter_viewmodel by activityViewModels {
            volunter_viewmodelFactory(repository)
        }
        viewModel.getStudentsFromFirestore()


        binding.addbutton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            }
            pickExcelFileLauncher.launch(intent)
        }

        viewModel.students.observe(viewLifecycleOwner) { students ->
            Log.d("volunter Fragment","Observed student list size: ${students.size}")
            if (adapter == null) {
                adapter = volunter_adapter(students)
                binding.studentRecyclerView.adapter = adapter
                binding.studentRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            } else {
                if (students != adapter?.studentdata) { // Check for changes
                    adapter?.studentdata?.clear()
                    adapter?.studentdata?.addAll(students)
                    adapter?.notifyDataSetChanged()
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                showErrorDialog(errorMessage)
            }
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading && viewModel.isLoading.hasActiveObservers()) { // Check for active observers
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
            }
        }






        pickExcelFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                excelFileUri = result.data?.data
                excelFileUri?.let { uri ->
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        viewModel.uploadExcelAndStoreData(inputStream) // Call the new function
                    }
                }
            }
        }
    }

    private fun showErrorDialog(errorMessage: String) {
        val dialogBinding = ErrorDialogBinding.inflate(LayoutInflater.from(requireContext()))
        dialogBinding.errorMessageTextView.text = errorMessage

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.openExcelButton.setOnClickListener {
            excelFileUri?.let { uri ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            }
            dialog.dismiss()
        }

        dialog.show()
    }





}
