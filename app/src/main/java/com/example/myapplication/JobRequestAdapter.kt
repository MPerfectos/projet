package com.example.myapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.myapplication.model.JobRequest

class JobRequestAdapter(
    private val context: Context,
    private val requests: List<JobRequest>
) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int = requests.size

    override fun getItem(position: Int): Any = requests[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: inflater.inflate(R.layout.item_request, parent, false)

        val request = requests[position]

        // Find views
        val jobTitle = view.findViewById<TextView>(R.id.jobTitle)
        val jobLocation = view.findViewById<TextView>(R.id.jobLocation)
        val jobType = view.findViewById<TextView>(R.id.jobType)
        val personName = view.findViewById<TextView>(R.id.personName)
        val salary = view.findViewById<TextView>(R.id.salary)
        val jobCategoryImage = view.findViewById<ImageView>(R.id.jobCategoryImage)
        val categoryCard = view.findViewById<CardView>(R.id.categoryCard)

        // Set data
        jobTitle.text = request.jobTitle
        jobLocation.text = "Audin, Algiers" // You can make this dynamic
        jobType.text = request.jobType
        personName.text = "By ${request.personName}"
        salary.text = "${request.salary}/h"

        // Set category-specific styling
        setCategoryStyle(request.jobTitle, jobCategoryImage, categoryCard)

        return view
    }

    private fun setCategoryStyle(jobTitle: String, imageView: ImageView, cardView: CardView) {
        when {
            jobTitle.lowercase().contains("software") ||
                    jobTitle.lowercase().contains("programming") ||
                    jobTitle.lowercase().contains("developer") -> {
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.skill_coding))
                imageView.setImageResource(R.drawable.ic_coding)
            }
            jobTitle.lowercase().contains("design") ||
                    jobTitle.lowercase().contains("graphic") ||
                    jobTitle.lowercase().contains("ui") -> {
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.skill_design))
                imageView.setImageResource(R.drawable.ic_design)
            }
            jobTitle.lowercase().contains("construction") ||
                    jobTitle.lowercase().contains("building") ||
                    jobTitle.lowercase().contains("engineer") -> {
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.skill_construction))
                imageView.setImageResource(R.drawable.ic_construction)
            }
            jobTitle.lowercase().contains("garden") ||
                    jobTitle.lowercase().contains("plant") ||
                    jobTitle.lowercase().contains("landscape") -> {
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.skill_gardening))
                imageView.setImageResource(R.drawable.ic_gardening)
            }
            else -> {
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.skill_design))
                imageView.setImageResource(R.drawable.ic_design)
            }
        }
    }
}