package com.example.myapplication

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import com.example.myapplication.model.Message
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val context: Context,
    private val messages: MutableList<Message>,
    private val currentUserId: String,
    private val onDeleteMessage: (Message) -> Unit // Callback for delete action
) : BaseAdapter() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.getDefault())

    override fun getCount(): Int = messages.size

    override fun getItem(position: Int): Any = messages[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View = convertView
            ?: LayoutInflater.from(context).inflate(R.layout.item_message, parent, false)

        val message = messages[position]
        val isSentByCurrentUser = message.senderId == currentUserId

        // Get all views
        val llSentMessage = view.findViewById<LinearLayout>(R.id.llSentMessage)
        val llReceivedMessage = view.findViewById<LinearLayout>(R.id.llReceivedMessage)
        val tvSentMessage = view.findViewById<TextView>(R.id.tvSentMessage)
        val tvReceivedMessage = view.findViewById<TextView>(R.id.tvReceivedMessage)
        val tvSentTime = view.findViewById<TextView>(R.id.tvSentTime)
        val tvReceivedTime = view.findViewById<TextView>(R.id.tvReceivedTime)

        // Hide both layouts first
        llSentMessage.visibility = View.GONE
        llReceivedMessage.visibility = View.GONE

        // Format timestamp
        val timeString = dateFormat.format(Date(message.timestamp))

        if (isSentByCurrentUser) {
            // Show sent message layout
            llSentMessage.visibility = View.VISIBLE
            tvSentMessage.text = message.content
            tvSentTime.text = timeString

            // Show timestamp for last few messages or on tap
            tvSentTime.visibility = if (shouldShowTimestamp(position)) View.VISIBLE else View.GONE

            // Set long click listener for sent messages
            llSentMessage.setOnLongClickListener {
                showContextMenu(it, message, isSentByCurrentUser)
                true
            }
        } else {
            // Show received message layout
            llReceivedMessage.visibility = View.VISIBLE
            tvReceivedMessage.text = message.content
            tvReceivedTime.text = timeString

            // Show timestamp for last few messages or on tap
            tvReceivedTime.visibility = if (shouldShowTimestamp(position)) View.VISIBLE else View.GONE

            // Set long click listener for received messages
            llReceivedMessage.setOnLongClickListener {
                showContextMenu(it, message, isSentByCurrentUser)
                true
            }
        }

        // Keep the original TextView for backward compatibility but hide it
        val tvMessage = view.findViewById<TextView>(R.id.tvMessage)
        tvMessage.visibility = View.GONE

        return view
    }

    private fun showContextMenu(view: View, message: Message, isSentByCurrentUser: Boolean) {
        val popupMenu = PopupMenu(context, view)

        // Always add copy option
        popupMenu.menu.add(0, 1, 0, "نسخ")

        // Add delete option only for messages sent by current user
        if (isSentByCurrentUser) {
            popupMenu.menu.add(0, 2, 0, "حذف")
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> {
                    // Copy message
                    copyMessageToClipboard(message.content)
                    true
                }
                2 -> {
                    // Delete message
                    onDeleteMessage(message)
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun copyMessageToClipboard(messageContent: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("رسالة", messageContent)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ الرسالة", Toast.LENGTH_SHORT).show()
    }

    private fun shouldShowTimestamp(position: Int): Boolean {
        // Show timestamp for the last message or every 5th message
        return position == messages.size - 1 || position % 5 == 0
    }

    // دالة لإضافة رسالة جديدة وتحديث الـ ListView
    fun addMessage(newMsg: Message) {
        messages.add(newMsg)
        notifyDataSetChanged()
    }
}