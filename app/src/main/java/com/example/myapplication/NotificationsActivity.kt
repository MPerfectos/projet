import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationsActivity : AppCompatActivity() {

    private lateinit var uid: String
    private lateinit var role: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationsAdapter
    private val notificationsList = mutableListOf<NotificationItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        uid = intent.getStringExtra("uid") ?: return
        role = intent.getStringExtra("role") ?: return

        recyclerView = findViewById(R.id.notificationsRecyclerView)
        adapter = NotificationsAdapter(this, notificationsList)
        recyclerView.adapter = adapter

        fetchNotifications()
    }

    private fun fetchNotifications() {
        val db = FirebaseFirestore.getInstance()
        db.collection("notifications")
            .whereEqualTo("uid", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "خطأ في تحميل الإشعارات", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                notificationsList.clear()
                for (doc in snapshots!!) {
                    val notification = doc.toObject(NotificationItem::class.java)
                    notificationsList.add(notification)
                }
                adapter.notifyDataSetChanged()
            }
    }
}
