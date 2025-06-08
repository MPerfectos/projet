package com.example.myapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.myapplication.R
import java.text.SimpleDateFormat
import java.util.*

class MyRequestsAdapter(
    private val context: Context,
    private val data: List<Pair<String, Map<String, Any>>>
) : BaseAdapter() {

    override fun getCount(): Int = data.size

    override fun getItem(position: Int): Any = data[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.my_request_item, parent, false)

        val jobTitleView = view.findViewById<TextView>(R.id.textJobTitle)
        val jobTypeView = view.findViewById<TextView>(R.id.textJobType)
        val jobInitialView = view.findViewById<TextView>(R.id.textJobInitial)
        val jobStatusView = view.findViewById<TextView>(R.id.textJobStatus)
        val requestDateView = view.findViewById<TextView>(R.id.textRequestDate)

        val (_, requestData) = data[position]
        val jobTitle = requestData["jobName"]?.toString() ?: "Unknown Job"
        val jobType = requestData["jobType"]?.toString() ?: "Unknown Type"
        val status = requestData["status"]?.toString() ?: "Pending"
        val timestamp = requestData["timestamp"] as? com.google.firebase.Timestamp

        // Set job title and type
        jobTitleView.text = jobTitle
        jobTypeView.text = jobType

        // Set job initial (first letter of job title)
        jobInitialView.text = if (jobTitle.isNotEmpty()) {
            jobTitle.first().uppercaseChar().toString()
        } else {
            "J"
        }

        // Set status with appropriate styling
        jobStatusView.text = status
        when (status.lowercase()) {
            "approved", "accepted" -> {
                jobStatusView.setBackgroundResource(R.drawable.status_approved_background)
                jobStatusView.setTextColor(context.getColor(android.R.color.white))
            }
            "rejected", "declined" -> {
                jobStatusView.setBackgroundResource(R.drawable.status_rejected_background)
                jobStatusView.setTextColor(context.getColor(android.R.color.white))
            }
            "pending" -> {
                jobStatusView.setBackgroundResource(R.drawable.status_badge_background)
                jobStatusView.setTextColor(context.getColor(R.color.orange_text))
            }
            else -> {
                jobStatusView.setBackgroundResource(R.drawable.status_badge_background)
                jobStatusView.setTextColor(context.getColor(R.color.orange_text))
            }
        }

        // Format and set request date
        if (timestamp != null) {
            val date = timestamp.toDate()
            val now = Date()
            val diffInMillis = now.time - date.time
            val diffInDays = diffInMillis / (24 * 60 * 60 * 1000)

            requestDateView.text = when {
                diffInDays < 1 -> "Today"
                diffInDays < 2 -> "Yesterday"
                diffInDays < 7 -> "${diffInDays.toInt()} days ago"
                else -> {
                    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                    dateFormat.format(date)
                }
            }
        } else {
            requestDateView.text = "Unknown date"
        }

        return view
    }
}