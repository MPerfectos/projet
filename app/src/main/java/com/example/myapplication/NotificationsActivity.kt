package com.example.myapplication

import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NotificationsActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var currentUid: String
    private val notificationsList = mutableListOf<NotificationData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        listView = findViewById(R.id.notificationsListView)
        currentUid = auth.currentUser?.uid ?: return

        loadNotifications()
    }

    private fun loadNotifications() {
        db.collection("notifications").get()
            .addOnSuccessListener { result ->
                notificationsList.clear()
                for (doc in result) {
                    val fromId = doc.getString("fromId") ?: continue
                    val toId = doc.getString("toId") ?: continue
                    val message = doc.getString("message") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: 0L

                    if (fromId == currentUid || toId == currentUid) {
                        val otherUserId = if (fromId == currentUid) toId else fromId
                        notificationsList.add(NotificationData(otherUserId, message, timestamp))
                    }
                }
                listView.adapter = NotificationAdapter(this, notificationsList)
            }
    }
}
