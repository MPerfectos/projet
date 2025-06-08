package com.example.myapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class ApplicantAdapter(
    context: Context,
    private val applicants: List<Pair<String, String>>
) : ArrayAdapter<Pair<String, String>>(context, 0, applicants) {


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val applicant = applicants[position]
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_applicant, parent, false)
        val tvName = view.findViewById<TextView>(R.id.tvApplicantName)
        tvName.text = applicant.second
        return view
    }
}
