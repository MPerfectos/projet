package com.example.myapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.myapplication.model.JobRequest

class JobRequestAdapter(
    private val context: Context,
    private val dataList: List<JobRequest>
) : BaseAdapter() {

    override fun getCount(): Int = dataList.size
    override fun getItem(position: Int): Any = dataList[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_request, parent, false)

        val personName = view.findViewById<TextView>(R.id.tvPersonName)
        val jobTitle = view.findViewById<TextView>(R.id.tvJobTitle)
        val jobType = view.findViewById<TextView>(R.id.tvJobType)
        val salary = view.findViewById<TextView>(R.id.tvSalary)

        val item = dataList[position]

        personName.text = item.personName
        jobTitle.text = item.jobTitle
        jobType.text = item.jobType
        salary.text = item.salary

        return view
    }
}
