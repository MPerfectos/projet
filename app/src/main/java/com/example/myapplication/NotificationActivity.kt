package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View
    private lateinit var tvMarkAllRead: TextView
    private lateinit var fabRefresh: FloatingActionButton
    private lateinit var toolbar: Toolbar

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var currentUid: String
    private val notificationsList = mutableListOf<NotificationData>()
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()

        currentUid = auth.currentUser?.uid ?: return
        loadNotifications()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.notificationsRecyclerView)
        emptyState = findViewById(R.id.emptyState)
        tvMarkAllRead = findViewById(R.id.tvMarkAllRead)
        fabRefresh = findViewById(R.id.fabRefresh)
        toolbar = findViewById(R.id.toolbar)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(this, notificationsList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Add item decoration for spacing
        val spacing = resources.getDimensionPixelSize(R.dimen.notification_item_spacing)
        recyclerView.addItemDecoration(SpacingItemDecoration(spacing))
    }

    private fun setupClickListeners() {
        tvMarkAllRead.setOnClickListener {
            markAllAsRead()
        }

        fabRefresh.setOnClickListener {
            refreshNotifications()
        }
    }

    private fun loadNotifications() {
        showLoading(true)

        db.collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
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

                adapter.notifyDataSetChanged()
                updateEmptyState()
                showLoading(false)
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                // Handle error - you might want to show a toast or snackbar
            }
    }

    private fun refreshNotifications() {
        // Add rotation animation to FAB
        fabRefresh.animate()
            .rotation(fabRefresh.rotation + 360f)
            .setDuration(500)
            .start()

        loadNotifications()
    }

    private fun markAllAsRead() {
        // Implement mark all as read functionality
        // This could involve updating Firestore documents or local state
        tvMarkAllRead.visibility = View.GONE
    }

    private fun updateEmptyState() {
        if (notificationsList.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            tvMarkAllRead.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            tvMarkAllRead.visibility = View.VISIBLE
        }
    }

    private fun showLoading(show: Boolean) {
        // You can implement a loading indicator here
        // For example, show/hide a progress bar
    }

    // Helper class for RecyclerView item spacing
    private class SpacingItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: android.graphics.Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.left = spacing
            outRect.right = spacing
            outRect.bottom = spacing

            // Add top margin only for the first item
            if (parent.getChildAdapterPosition(view) == 0) {
                outRect.top = spacing
            }
        }
    }
}