package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.ChatAdapter
import com.example.myapplication.model.ChatPreview
import com.google.firebase.firestore.FirebaseFirestore

class ChatsActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var firestore: FirebaseFirestore
    private var userId: String? = null
    private var role: String? = null
    private val chatList = mutableListOf<ChatPreview>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats)

        listView = findViewById(R.id.chatsListView)
        firestore = FirebaseFirestore.getInstance()
        userId = intent.getStringExtra("uid")
        role = intent.getStringExtra("role")

        if (userId == null || role == null) {
            Toast.makeText(this, "فشل في تحميل بيانات المستخدم", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadChats()

        // Remove the ListView.setOnItemClickListener as it conflicts with the adapter's click listener
        // The click handling is now done in the ChatAdapter

        val navhome = findViewById<LinearLayout>(R.id.navHome)
        navhome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val navprofil = findViewById<LinearLayout>(R.id.navProfile)
        navprofil.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        val navmap = findViewById<LinearLayout>(R.id.navMap)
        navmap.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }

        val navnotification = findViewById<LinearLayout>(R.id.navNotifications)
        navnotification.setOnClickListener {
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadChats() {
        firestore.collection("chats")
            .get()
            .addOnSuccessListener { result ->
                chatList.clear()
                for (doc in result) {
                    val user1 = doc.getString("user1")
                    val user2 = doc.getString("user2")

                    if (user1 == userId || user2 == userId) {
                        val otherId = if (user1 == userId) user2 else user1
                        if (otherId != null) {
                            // ✅ اجلب اسم المستخدم الآخر بدلاً من ID
                            firestore.collection("users").document(otherId)
                                .get()
                                .addOnSuccessListener { userDoc ->
                                    val otherName = userDoc.getString("name") ?: "مستخدم"
                                    chatList.add(ChatPreview(chatId = doc.id, otherUserId = otherName))

                                    // Create adapter with callback function for chat clicks
                                    listView.adapter = ChatAdapter(this, chatList) { selectedChat ->
                                        // This callback is called when a chat is clicked
                                        val intent = Intent(this, ChatRoomActivity::class.java)
                                        intent.putExtra("chatId", selectedChat.chatId)
                                        intent.putExtra("userId", userId)
                                        startActivity(intent)
                                    }
                                }
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "فشل في تحميل المحادثات", Toast.LENGTH_SHORT).show()
            }
    }
}