package com.example.myapplication

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class NotificationAdapter(
    private val context: Context,
    private val notifications: MutableList<NotificationData>
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val userNameCache = mutableMapOf<String, String>()

    // Predefined colors for user avatars
    private val avatarColors = arrayOf(
        "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7",
        "#DDA0DD", "#98D8C8", "#FD79A8", "#6C5CE7", "#A29BFE"
    )

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserInitial: TextView = itemView.findViewById(R.id.tvUserInitial)
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val notificationDot: View = itemView.findViewById(R.id.notificationDot)
        val cardView: androidx.cardview.widget.CardView = itemView as androidx.cardview.widget.CardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]

        // Check cache first
        val cachedName = userNameCache[notification.otherUserId]
        if (cachedName != null) {
            setupUserInfo(holder, cachedName, notification)
        } else {
            // Fetch from Firestore and cache
            db.collection("users").document(notification.otherUserId).get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("name") ?: "مستخدم"
                    userNameCache[notification.otherUserId] = name
                    setupUserInfo(holder, name, notification)
                }
                .addOnFailureListener {
                    setupUserInfo(holder, "مستخدم", notification)
                }
        }

        // Set message with proper styling
        holder.tvMessage.text = notification.message

        // Format and set timestamp
        val timeStr = formatTime(notification.timestamp)
        holder.tvTime.text = timeStr

        // Add subtle hover effect
        holder.cardView.setOnClickListener {
            // Handle notification click
            onNotificationClick(notification, position)
        }

        // Add long press for additional options
        holder.cardView.setOnLongClickListener {
            onNotificationLongClick(notification, position)
            true
        }
    }

    private fun setupUserInfo(holder: NotificationViewHolder, name: String, notification: NotificationData) {
        holder.tvUserName.text = name

        // Set user initial and avatar color
        val initial = if (name.isNotEmpty()) name.first().toString().uppercase() else "م"
        holder.tvUserInitial.text = initial

        // Set consistent color based on user ID
        val colorIndex = notification.otherUserId.hashCode() % avatarColors.size
        val color = Color.parseColor(avatarColors[Math.abs(colorIndex)])
        holder.tvUserInitial.setBackgroundColor(color)
    }

    private fun formatTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60 * 1000 -> "الآن"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} دقيقة"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} ساعة"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} يوم"
            else -> {
                val sdf = SimpleDateFormat("d MMM yyyy", Locale("ar"))
                sdf.format(Date(timestamp))
            }
        }
    }

    private fun onNotificationClick(notification: NotificationData, position: Int) {
        // Hide notification indicator
        // You can add navigation logic here
        notifyItemChanged(position)
    }

    private fun onNotificationLongClick(notification: NotificationData, position: Int) {
        // Show context menu with options like delete, mark as read, etc.
        val popup = PopupMenu(context, null)
        // Add menu items and handle selection
    }

    override fun getItemCount(): Int = notifications.size

    // Helper method to add new notification with animation
    fun addNotification(notification: NotificationData) {
        notifications.add(0, notification)
        notifyItemInserted(0)
    }

    // Helper method to remove notification with animation
    fun removeNotification(position: Int) {
        if (position >= 0 && position < notifications.size) {
            notifications.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    // Helper method to clear all notifications
    fun clearAll() {
        val size = notifications.size
        notifications.clear()
        notifyItemRangeRemoved(0, size)
    }
}