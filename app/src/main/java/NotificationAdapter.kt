package com.example.myapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val context: Context,
    private val notifications: List<NotificationData>
) : BaseAdapter() {

    private val db = FirebaseFirestore.getInstance()

    override fun getCount(): Int = notifications.size

    override fun getItem(position: Int): Any = notifications[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_notification, parent, false)

        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)
        val tvMessage = view.findViewById<TextView>(R.id.tvMessage)
        val tvTime = view.findViewById<TextView>(R.id.tvTime)

        val notification = notifications[position]


        db.collection("users").document(notification.otherUserId).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "مستخدم"
                tvUserName.text = name
            }

        tvMessage.text = notification.message

        val sdf = SimpleDateFormat(" MMM yyyy - HH:mm", Locale("ar"))
        val dateStr = sdf.format(Date(notification.timestamp))
        tvTime.text = dateStr

        return view
    }
}
