package com.example.data_entry_android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.data_entry_android.databinding.MainpageContentBinding

class ContentActivity : AppCompatActivity() {

    private lateinit var binding: MainpageContentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainpageContentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the support action bar
        setSupportActionBar(binding.toolbar)

        // Set the title of the activity
        supportActionBar?.title = "Main Page"

        // Set click listeners for the buttons
        binding.enterDataButton.setOnClickListener {
            val intent = Intent(this, DataEntryActivity::class.java)
            startActivity(intent)
        }

        binding.viewDataButton.setOnClickListener {
            val intent = Intent(this, DataSavedActivity::class.java)
            startActivity(intent)
        }

        binding.viewLeaderboardButton.setOnClickListener {
            val intent = Intent(this, LeaderboardActivity::class.java)
            startActivity(intent)
        }
    }
}