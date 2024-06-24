package com.example.data_entry_android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.data_entry_android.databinding.ActivityLeaderboardBinding
import com.example.data_entry_android.model.Entry

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLeaderboardBinding
    private lateinit var adapter: LeaderboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLeaderboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Leaderboard"

        // Set up the RecyclerView
        binding.leaderboardRecyclerView.layoutManager = LinearLayoutManager(this)

        // Load entries and calculate leaderboard
        val dbHelper = DatabaseHelper(this)
        val entries = dbHelper.getAllEntries()
        val leaderboard = calculateLeaderboard(entries)

        // Initialize the adapter with leaderboard data
        adapter = LeaderboardAdapter(leaderboard)
        binding.leaderboardRecyclerView.adapter = adapter

        // Set up the back button
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun calculateLeaderboard(entries: List<Entry>): List<UserTotal> {
        val totals = mutableMapOf<String, UserTotal>()

        for (entry in entries) {
            val sum = entry.items.sumOf { it.second.toDoubleOrNull() ?: 0.0 }
            if (totals.containsKey(entry.userName)) {
                totals[entry.userName]?.let {
                    it.total += sum
                    it.entries.add(entry)
                }
            } else {
                totals[entry.userName] = UserTotal(entry.userName, sum, mutableListOf(entry))
            }
        }

        return totals.values.sortedByDescending { it.total }
    }
}

data class UserTotal(
    val name: String,
    var total: Double,
    val entries: MutableList<Entry>
)

class LeaderboardAdapter(private val leaderboard: List<UserTotal>) :
    RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rankTextView: TextView = view.findViewById(R.id.rankTextView)
        val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        val totalTextView: TextView = view.findViewById(R.id.totalTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val userTotal = leaderboard[position]
        holder.rankTextView.text = "${position + 1}."
        holder.nameTextView.text = userTotal.name
        holder.totalTextView.text = String.format("%.2f", userTotal.total)
    }

    override fun getItemCount() = leaderboard.size
}