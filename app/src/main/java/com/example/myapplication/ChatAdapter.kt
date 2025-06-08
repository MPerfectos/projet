package com.example.myapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.myapplication.R
import com.example.myapplication.model.ChatPreview

class ChatAdapter(
    private val context: Context,
    private val chats: List<ChatPreview>,
    private val onChatClick: (ChatPreview) -> Unit = {} // Callback for chat clicks
) : BaseAdapter() {

    override fun getCount(): Int = chats.size
    override fun getItem(position: Int): Any = chats[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val holder: ViewHolder
        val view: View

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.chat_item_layout, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val chat = chats[position]

        // Set user name using your existing ChatPreview fields
        holder.contactName.text = "User: ${chat.otherUserId}"

        // Set default message preview
        holder.messagePreview.text = "Tap to start chatting..."

        // Set default timestamp
        holder.timestamp.text = "now"

        // Set default profile image
        holder.profileImage.setImageResource(R.drawable.default_avatar)

        // Hide optional UI elements by default
        holder.messageCountBadge.visibility = View.GONE
        holder.onlineIndicator.visibility = View.GONE
        holder.actionButtons.visibility = View.GONE

        // Handle regular click to open chat room
        view.setOnClickListener {
            // Hide action buttons if they're visible
            if (holder.actionButtons.visibility == View.VISIBLE) {
                holder.actionButtons.visibility = View.GONE
                holder.timestamp.visibility = View.VISIBLE
            } else {
                // Open chat room using the callback
                onChatClick(chat)
            }
        }

        // Handle long press to show action buttons
        view.setOnLongClickListener {
            if (holder.actionButtons.visibility == View.GONE) {
                holder.actionButtons.visibility = View.VISIBLE
                holder.timestamp.visibility = View.GONE
            } else {
                holder.actionButtons.visibility = View.GONE
                holder.timestamp.visibility = View.VISIBLE
            }
            true
        }

        // Handle action button clicks
        holder.muteButton.setOnClickListener {
            // TODO: Implement mute/unmute functionality
            // You can add a muted field to your ChatPreview model
        }

        holder.deleteButton.setOnClickListener {
            // TODO: Implement delete functionality
            // You can use chat.chatId to delete from Firestore
            // Example: deleteChat(chat.chatId)
        }

        return view
    }

    // ViewHolder pattern for better performance
    private class ViewHolder(view: View) {
        val profileImage: ImageView = view.findViewById(R.id.profileImage)
        val contactName: TextView = view.findViewById(R.id.contactName)
        val messagePreview: TextView = view.findViewById(R.id.messagePreview)
        val timestamp: TextView = view.findViewById(R.id.timestamp)
        val messageCountBadge: TextView = view.findViewById(R.id.messageCountBadge)
        val onlineIndicator: View = view.findViewById(R.id.onlineIndicator)
        val muteButton: ImageView = view.findViewById(R.id.muteButton)
        val deleteButton: ImageView = view.findViewById(R.id.deleteButton)
        val actionButtons: LinearLayout = view.findViewById(R.id.actionButtons)
    }
}