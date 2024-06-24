package com.example.data_entry_android


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.data_entry_android.databinding.ActivityDataSavedBinding
import com.example.data_entry_android.model.Entry
import com.example.data_entry_android.DatabaseHelper


class DataSavedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDataSavedBinding
    private lateinit var adapter: EntriesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDataSavedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the RecyclerView
        binding.entriesRecyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize the adapter with an empty list
        adapter = EntriesAdapter(emptyList())
        binding.entriesRecyclerView.adapter = adapter

        // Load and display the entries
        loadEntries()


        supportActionBar?.title = "Saved Data"

        // Set up the back button
        binding.backButton.setOnClickListener {
            finish()  // This will close the current activity and return to the previous one
        }
    }

    private fun loadEntries() {
        val dbHelper = DatabaseHelper(this)
        dbHelper.getAllEntries { entries, error ->
            if (entries != null) {
                // Now you can work with your entries list
                println("Loaded ${entries.size} entries")
                entries.forEachIndexed { index, entry ->
                    println("Entry $index: ${entry.userName}, ${entry.timestamp}, ${entry.items.size} items")
                    entry.items.forEach { (description, value) ->
                        println("  Item: $description - $value")
                    }
                }
                // Assuming 'adapter' is initialized somewhere in your Activity
                adapter.updateEntries(entries)
            } else if (error != null) {
                // Handle the error appropriately
                println("Error loading entries: $error")
            }
        }
    }
    inner class EntriesAdapter(private var entries: List<Entry>) :
        RecyclerView.Adapter<EntriesAdapter.EntryViewHolder>() {

        private val expandedPositions = mutableSetOf<Int>()

        fun updateEntries(newEntries: List<Entry>) {
            entries = newEntries
            expandedPositions.clear()
            notifyDataSetChanged()
        }

        inner class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
            val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
            val detailsContainer: ViewGroup = itemView.findViewById(R.id.detailsContainer)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_entry, parent, false)
            return EntryViewHolder(view)
        }

        override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
            val entry = entries[position]
            holder.userNameTextView.text = entry.userName
            holder.timestampTextView.text = entry.timestamp

            val isExpanded = expandedPositions.contains(position)
            updateDetailsVisibility(holder, isExpanded, entry)

            holder.itemView.setOnClickListener {
                if (isExpanded) {
                    expandedPositions.remove(position)
                } else {
                    expandedPositions.add(position)
                }
                notifyItemChanged(position)
                println("Item clicked at position $position, expanded: ${!isExpanded}")
            }
        }

        private fun updateDetailsVisibility(holder: EntryViewHolder, isExpanded: Boolean, entry: Entry) {
            holder.detailsContainer.removeAllViews()
            if (isExpanded) {
                for ((description, value) in entry.items) {
                    val detailView = LayoutInflater.from(holder.itemView.context)
                        .inflate(R.layout.item_entry_detail, holder.detailsContainer, false)
                    detailView.findViewById<TextView>(R.id.descriptionTextView).text = "Description: $description"
                    detailView.findViewById<TextView>(R.id.valueTextView).text = "Value: $value"
                    holder.detailsContainer.addView(detailView)
                }
                holder.detailsContainer.visibility = View.VISIBLE
            } else {
                holder.detailsContainer.visibility = View.GONE
            }
            println("Updated details visibility for position ${holder.adapterPosition}, expanded: $isExpanded")
        }

        override fun getItemCount() = entries.size
    }
}