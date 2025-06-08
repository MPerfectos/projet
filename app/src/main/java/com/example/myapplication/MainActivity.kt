package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AbsListView
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.model.JobRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var btnCreateRequest: Button
    private lateinit var btnViewMyRequests: Button
    private lateinit var listView: ListView
    private lateinit var searchBar: EditText

    private var currentPage = 0
    private val itemsPerPage = 5
    private var allRequests = listOf<JobRequest>()
    private var filteredRequests = listOf<JobRequest>()
    private var isLoadingPage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        btnCreateRequest = findViewById(R.id.btnNewRequest)
        btnViewMyRequests = findViewById(R.id.btnMyRequests)
        listView = findViewById(R.id.listView)
        searchBar = findViewById(R.id.searchEditText)

        if (!isConnected()) {
            Toast.makeText(this, "لا يوجد اتصال بالإنترنت", Toast.LENGTH_LONG).show()
            return
        }

        checkUserRole()

        btnCreateRequest.setOnClickListener {
            val intent = Intent(this, CreateRequestActivity::class.java)
            intent.putExtra("uid", auth.currentUser?.uid)
            startActivity(intent)
        }

        btnViewMyRequests.setOnClickListener {
            val intent1 = Intent(this, MyRequestsActivity::class.java)
            intent1.putExtra("uid", auth.currentUser?.uid)
            startActivity(intent1)
        }

        searchBar.setOnEditorActionListener { _, actionId, event ->
            val isEnterPressed = (event != null
                    && event.keyCode == KeyEvent.KEYCODE_ENTER
                    && event.action == KeyEvent.ACTION_DOWN)
            if (actionId == EditorInfo.IME_ACTION_SEARCH || isEnterPressed) {
                performSearch(searchBar.text.toString())
                true
            } else {
                false
            }
        }

        listView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {}

            override fun onScroll(
                view: AbsListView?,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {
                if (!isLoadingPage && (firstVisibleItem + visibleItemCount >= totalItemCount)
                    && totalItemCount > 0 && totalItemCount < filteredRequests.size
                ) {
                    currentPage++
                    updateListView()
                }
            }
        })

        val navChats = findViewById<LinearLayout>(R.id.navChats)
        navChats.setOnClickListener {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                firestore.collection("users").document(uid).get()
                    .addOnSuccessListener { document ->
                        val role = document.getString("role") ?: ""
                        val intent = Intent(this, ChatsActivity::class.java).apply {
                            putExtra("uid", uid)
                            putExtra("role", role)
                        }
                        startActivity(intent)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "فشل في جلب بيانات المستخدم", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "المستخدم غير مسجل الدخول", Toast.LENGTH_SHORT).show()
            }
        }
        val navprofiles = findViewById<LinearLayout>(R.id.navProfile)
        navprofiles.setOnClickListener {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                firestore.collection("users").document(uid).get()
                    .addOnSuccessListener { document ->
                        val role = document.getString("role") ?: ""
                        val intent = Intent(this, MyProfileActivity::class.java).apply {
                            putExtra("uid", uid)
                            putExtra("role", role)
                        }
                        startActivity(intent)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "فشل في جلب بيانات المستخدم", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "المستخدم غير مسجل الدخول", Toast.LENGTH_SHORT).show()
            }
        }
        val navnotification = findViewById<LinearLayout>(R.id.navNotifications)
        navnotification.setOnClickListener {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                firestore.collection("users").document(uid).get()
                    .addOnSuccessListener { document ->
                        val role = document.getString("role") ?: ""
                        val intent = Intent(this, NotificationsActivity::class.java).apply {
                            putExtra("uid", uid)
                            putExtra("role", role)
                        }
                        startActivity(intent)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "فشل في جلب بيانات المستخدم", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "المستخدم غير مسجل الدخول", Toast.LENGTH_SHORT).show()
            }
        }


















































    }

    private fun isConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun checkUserRole() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "المستخدم غير مسجل الدخول", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val role = document.getString("role")
                if (role == "employee") {
                    btnCreateRequest.visibility = View.GONE
                    btnViewMyRequests.visibility = View.GONE
                } else {
                    btnCreateRequest.visibility = View.VISIBLE
                    btnViewMyRequests.visibility = View.VISIBLE
                }
                fetchAllRequests()
            }
            .addOnFailureListener {
                Toast.makeText(this, "فشل في الحصول على الدور", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchAllRequests() {
        firestore.collection("requests").get()
            .addOnSuccessListener { result ->
                allRequests = result.map { doc ->
                    JobRequest(
                        personName =( doc.getString("userName") ?: ""),
                        jobTitle ="Jop Name : " + ( doc.getString("jobName") ?: ""),
                        jobType = "Type : "+(doc.getString("jobType") ?: ""),
                        salary = "Salary : "+(doc.getString("price") ?: ""),
                        createdByUid = doc.getString("uid") ?: "",
                        requestId = doc.id
                    )
                }
                filteredRequests = allRequests
                currentPage = 0
                updateListView()
            }
            .addOnFailureListener {
                Toast.makeText(this, "فشل في تحميل الطلبات", Toast.LENGTH_SHORT).show()
            }
    }

    private fun performSearch(query: String) {
        val lowerQuery = query.lowercase().trim()
        filteredRequests = if (lowerQuery.isEmpty()) {
            allRequests
        } else {
            allRequests.filter { item ->
                item.personName.lowercase().contains(lowerQuery) ||
                        item.jobTitle.lowercase().contains(lowerQuery) ||
                        item.jobType.lowercase().contains(lowerQuery) ||
                        item.salary.lowercase().contains(lowerQuery)
            }
        }
        currentPage = 0
        updateListView()
    }

    private fun updateListView() {
        isLoadingPage = true

        val fromIndex = currentPage * itemsPerPage
        val toIndex = minOf(fromIndex + itemsPerPage, filteredRequests.size)

        if (fromIndex >= filteredRequests.size) {
            isLoadingPage = false
            return
        }

        val sublist = filteredRequests.subList(0, toIndex)

        val adapter = JobRequestAdapter(this, sublist)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val selected = sublist[position]
            val intent = Intent(this, RequestDetailsActivity::class.java).apply {
                putExtra("currentUid", auth.currentUser?.uid)
                putExtra("creatorUid", selected.createdByUid)
                putExtra("requestId", selected.requestId)
            }
            startActivity(intent)
        }

        isLoadingPage = false
    }
}
