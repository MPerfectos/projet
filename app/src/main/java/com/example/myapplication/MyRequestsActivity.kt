package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class MyRequestsActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var db: FirebaseFirestore
    private lateinit var uid: String
    private lateinit var requests: MutableList<Pair<String, Map<String, Any>>>
    private lateinit var textTotalRequests: TextView
    private lateinit var textActiveRequests: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_requests)

        // Initialize views
        listView = findViewById(R.id.listViewMyRequests)
        textTotalRequests = findViewById(R.id.textTotalRequests)
        textActiveRequests = findViewById(R.id.textActiveRequests)

        db = FirebaseFirestore.getInstance()

        uid = intent.getStringExtra("uid") ?: ""
        if (uid.isEmpty()) {
            Toast.makeText(this, "لم يتم تمرير uid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadMyRequests()
    }

    private fun loadMyRequests() {
        db.collection("requests")
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { result ->
                requests = mutableListOf()
                var activeCount = 0

                for (doc in result) {
                    val requestData = doc.data
                    requests.add(Pair(doc.id, requestData))

                    // Count active requests (pending or approved)
                    val status = requestData["status"]?.toString()?.lowercase() ?: "pending"
                    if (status == "pending" || status == "approved" || status == "accepted") {
                        activeCount++
                    }
                }

                // Update stats
                textTotalRequests.text = requests.size.toString()
                textActiveRequests.text = activeCount.toString()

                // Set up adapter
                val adapter = MyRequestsAdapter(this, requests)
                listView.adapter = adapter

                listView.setOnItemClickListener { _, _, position, _ ->
                    val (requestId, _) = requests[position]
                    val intent = Intent(this, MyThesRequestActivity::class.java).apply {
                        putExtra("uid", uid)
                        putExtra("requestId", requestId)
                    }
                    startActivity(intent)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "فشل في تحميل الطلبات", Toast.LENGTH_SHORT).show()
            }
    }
}