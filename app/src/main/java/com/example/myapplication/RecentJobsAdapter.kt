package com.example.myapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import java.text.NumberFormat
import java.util.*

class RecentJobsAdapter(
    private val context: Context,
    private val jobs: List<Map<String, Any>>,
    private val userRole: String
) : BaseAdapter() {

    override fun getCount(): Int = jobs.size

    override fun getItem(position: Int): Any = jobs[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_recent_job, parent, false)

        val job = jobs[position]

        val jobImage = view.findViewById<ImageView>(R.id.jobImage)
        val jobTitle = view.findViewById<TextView>(R.id.jobTitle)
        val jobSubtitle = view.findViewById<TextView>(R.id.jobSubtitle)
        val jobAmount = view.findViewById<TextView>(R.id.jobAmount)

        // Set job title
        jobTitle.text = job["title"]?.toString() ?: "Unknown Job"

        if (userRole == "employee") {
            // For employees, show employer name and amount earned
            val employer = job["employer"]?.toString() ?: "Unknown Employer"
            val amount = job["amount"] as? Double ?: 0.0

            jobSubtitle.text = employer

            val formatter = NumberFormat.getNumberInstance(Locale.getDefault())
            jobAmount.text = "${formatter.format(amount)} DZD"
            jobAmount.visibility = View.VISIBLE

            // Set employee job icon
            jobImage.setImageResource(R.drawable.ic_work)
        } else {
            // For employers, show status
            val status = job["status"]?.toString() ?: "pending"
            val price = job["price"]?.toString() ?: "0"

            jobSubtitle.text = when (status.lowercase()) {
                "pending" -> "Pending approval"
                "approved", "accepted" -> "Active"
                "completed" -> "Completed"
                "cancelled" -> "Cancelled"
                else -> "Unknown status"
            }

            jobAmount.text = "$price DZD"
            jobAmount.visibility = View.VISIBLE

            // Set employer job icon
            jobImage.setImageResource(R.drawable.ic_work)
        }

        return view
    }
}