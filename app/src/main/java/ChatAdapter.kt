package com.example.myapplication.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.myapplication.R
import com.example.myapplication.model.ChatPreview

class ChatAdapter(private val context: Context, private val chats: List<ChatPreview>) : BaseAdapter() {
    override fun getCount(): Int = chats.size
    override fun getItem(position: Int): Any = chats[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.chat_item, parent, false)
        val chat = chats[position]
        val otherUserText = view.findViewById<TextView>(R.id.chatUserName)
        otherUserText.text = "User: ${chat.otherUserId}" // يمكن لاحقًا تحميل الاسم من Firestore
        return view
    }
}
