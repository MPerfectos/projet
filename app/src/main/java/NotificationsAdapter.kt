import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsAdapter(
    private val context: Context,
    private val notifications: List<NotificationItem>
) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageTextView: TextView = view.findViewById(R.id.bodyTextView)
        val timestampTextView: TextView = view.findViewById(R.id.timestampTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.timestampTextView.text = formatTimestamp(notification.timestamp)

        // جلب اسم المستخدم من fromId
        FirebaseFirestore.getInstance().collection("users")
            .document(notification.fromId)
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "مستخدم مجهول"
                holder.messageTextView.text = "$name: ${notification.message}"
            }
            .addOnFailureListener {
                holder.messageTextView.text = "مجهول: ${notification.message}"
            }
    }

    override fun getItemCount(): Int = notifications.size

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
