package com.example.myapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

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

        val (_, requestData) = data[position]
        val jobTitle = requestData["jobName"]?.toString() ?: ""
        val jobType = requestData["jobType"]?.toString() ?: ""

        jobTitleView.text ="Name Jop :" + jobTitle
        jobTypeView.text = "Type Jop :" + jobType

        return view
    }
}
