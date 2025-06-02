package com.example.myapplication.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.myapplication.R
import com.example.myapplication.model.Message

class MessageAdapter(
    private val context: Context,
    private val messages: MutableList<Message>,
    private val currentUserId: String
) : BaseAdapter() {

    override fun getCount(): Int = messages.size

    override fun getItem(position: Int): Any = messages[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View = convertView
            ?: LayoutInflater.from(context).inflate(R.layout.item_message, parent, false)

        val tvMessage = view.findViewById<TextView>(R.id.tvMessage)
        val message = messages[position]

        // إذا كان المرسل هو المستخدم الحالي، نضيف بادئة "أنا: " قبل المحتوى
        // وإلا نضيف بادئة "مي: " قبل المحتوى
        tvMessage.text = if (message.senderId == currentUserId) {
            "أنا: ${message.content}"
        } else {
            "هو : ${message.content}"
        }

        return view
    }

    // دالة لإضافة رسالة جديدة وتحديث الـ ListView
    fun addMessage(newMsg: Message) {
        messages.add(newMsg)
        notifyDataSetChanged()
    }
}
