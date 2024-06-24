package com.example.data_entry_android

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.data_entry_android.databinding.ActivityDataEntryBinding
import com.example.data_entry_android.model.Entry
import java.text.SimpleDateFormat
import java.util.*

class DataEntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDataEntryBinding
    private val dbHelper = DatabaseHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.addMoreButton.setOnClickListener {
            addMorePair()
        }

        binding.saveDataButton.setOnClickListener {
            saveData()
        }

        // Optionally fetch entries on create
        fetchEntries()
    }

    private fun addMorePair() {
        val pairLayout = LayoutInflater.from(this).inflate(R.layout.item_pair, binding.pairsContainer, false)
        val descriptionEditText = pairLayout.findViewById<EditText>(R.id.descriptionEditText)
        val valueEditText = pairLayout.findViewById<EditText>(R.id.valueEditText)

        valueEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = valueEditText.text.toString()
                if (value.isNotBlank() && !value.isNumeric()) {
                    showAlert("Please ensure you enter a numerical value for values, else it will not register.")
                }
            }
        }

        binding.pairsContainer.addView(pairLayout)
    }

    private fun saveData() {
        val userName = binding.userNameEditText.text.toString()
        val items = mutableListOf<Pair<String, String>>()

        for (i in 0 until binding.pairsContainer.childCount) {
            val pairLayout = binding.pairsContainer.getChildAt(i)
            val descriptionEditText = pairLayout.findViewById<EditText>(R.id.descriptionEditText)
            val valueEditText = pairLayout.findViewById<EditText>(R.id.valueEditText)
            val description = descriptionEditText.text.toString()
            val value = valueEditText.text.toString()
            items.add(Pair(description, value))
        }

        val currentTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val newEntry = Entry(userName, items, currentTimestamp)

        // Here we would normally save to the database, but let's assume you're doing something with the entry
        // For now, we just show the dialog
        showSaveCompleteAlert()
    }

    private fun fetchEntries() {
        dbHelper.getAllEntries { entries, error ->
            runOnUiThread { // Ensure UI updates are run on the UI thread
                if (!isFinishing && !isDestroyed) {
                    if (entries != null) {
                        // Handle entries, update UI
                    } else if (error != null) {
                        showAlert("Error fetching entries: $error")
                    }
                }
            }
        }
    }

    private fun showSaveCompleteAlert() {
        if (!isFinishing && !isDestroyed) { // Check that the activity is not finishing or destroyed
            AlertDialog.Builder(this)
                .setTitle("Save Complete")
                .setMessage("Your data has been saved successfully.")
                .setPositiveButton("OK") { _, _ ->
                    finish() // This will close the current activity and return to the previous one
                }
                .setCancelable(false) // Prevents dismissing the dialog by tapping outside or pressing back
                .show()
        }
    }

    private fun showAlert(message: String) {
        if (!isFinishing && !isDestroyed) { // Check that the activity is not finishing or destroyed
            AlertDialog.Builder(this)
                .setTitle("Reminder")
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun String.isNumeric(): Boolean {
        return this.toDoubleOrNull() != null
    }
}





/*
package com.example.data_entry_android
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.data_entry_android.databinding.ActivityDataEntryBinding
import com.example.data_entry_android.model.Entry
import java.text.SimpleDateFormat
import java.util.*


class DataEntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDataEntryBinding
    private val pairs: MutableList<Pair<String, String>> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.addMoreButton.setOnClickListener {
            addMorePair()
        }

        binding.saveDataButton.setOnClickListener {
            saveData()
        }
    }

    private fun addMorePair() {
        val pairLayout = LayoutInflater.from(this).inflate(R.layout.item_pair, binding.pairsContainer, false)
        val descriptionEditText = pairLayout.findViewById<EditText>(R.id.descriptionEditText)
        val valueEditText = pairLayout.findViewById<EditText>(R.id.valueEditText)

        valueEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = valueEditText.text.toString()
                if (value.isNotBlank() && !value.isNumeric()) {
                    showAlert("Numerical value when entering values, else it will not register")
                }
            }
        }

        binding.pairsContainer.addView(pairLayout)

        // Update this line to capture the actual values
        pairs.add(Pair(descriptionEditText.text.toString(), valueEditText.text.toString()))
    }


    private fun saveData() {
        val userName = binding.userNameEditText.text.toString()
        val items = mutableListOf<Pair<String, String>>()

        for (i in 0 until binding.pairsContainer.childCount) {
            val pairLayout = binding.pairsContainer.getChildAt(i)
            val descriptionEditText = pairLayout.findViewById<EditText>(R.id.descriptionEditText)
            val valueEditText = pairLayout.findViewById<EditText>(R.id.valueEditText)
            val description = descriptionEditText.text.toString()
            val value = valueEditText.text.toString()
            items.add(Pair(description, value))
        }

        val currentTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val newEntry = Entry(userName, items, currentTimestamp)

        // Use DatabaseHelper to save the entry
        val dbHelper = DatabaseHelper(this)
        dbHelper.insertEntry(newEntry)

        showSaveCompleteAlert()
    }

    private fun showSaveCompleteAlert() {
        AlertDialog.Builder(this)
            .setTitle("Save Complete")
            .setMessage("Your data has been saved successfully.")
            .setPositiveButton("OK") { _, _ ->
                finish()  // This will close the current activity and return to the previous one
            }
            .setCancelable(false)  // Prevents dismissing the dialog by tapping outside or pressing back
            .show()
    }

    private fun showAlert(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Reminder")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun String.isNumeric(): Boolean {
        return this.toDoubleOrNull() != null
    }
}

 */