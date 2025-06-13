package com.example.myapplication

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
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

    // Navigation items
    private lateinit var navHome: LinearLayout
    private lateinit var navMap: LinearLayout
    private lateinit var navNotifications: LinearLayout
    private lateinit var navChats: LinearLayout
    private lateinit var navProfile: LinearLayout

    private var currentPage = 0
    private val itemsPerPage = 10
    private var allRequests = listOf<JobRequest>()
    private var filteredRequests = listOf<JobRequest>()
    private var displayedRequests = mutableListOf<JobRequest>()
    private var isLoadingPage = false
    private var hasMoreData = true

    private var adapter: JobRequestAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }

        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        initializeViews()
        setupOptimizedAnimations()

        if (!isConnected()) {
            showStyledToast("No internet connection available")
            return
        }

        checkUserRole()
        setupClickListeners()
        setupSearchFunctionality()
        setupOptimizedScrollListener()
        setupNavigation()
    }

    private fun initializeViews() {
        btnCreateRequest = findViewById(R.id.btnNewRequest)
        btnViewMyRequests = findViewById(R.id.btnMyRequests)
        listView = findViewById(R.id.listView)
        searchBar = findViewById(R.id.searchEditText)

        navHome = findViewById(R.id.navHome)
        navMap = findViewById(R.id.navMap)
        navNotifications = findViewById(R.id.navNotifications)
        navChats = findViewById(R.id.navChats)
        navProfile = findViewById(R.id.navProfile)
    }

    private fun setupOptimizedAnimations() {
        searchBar.setOnFocusChangeListener { _, hasFocus ->
            val scale = if (hasFocus) 1.01f else 1.0f
            searchBar.parent.let { parent ->
                ObjectAnimator.ofFloat(parent as View, "scaleX", scale).apply {
                    duration = 150
                    start()
                }
                ObjectAnimator.ofFloat(parent, "scaleY", scale).apply {
                    duration = 150
                    start()
                }
            }
        }
    }

    private fun setupClickListeners() {
        btnCreateRequest.setOnClickListener {
            animateButtonPressOptimized(it)
            val intent = Intent(this, CreateRequestActivity::class.java)
            intent.putExtra("uid", auth.currentUser?.uid)
            startActivity(intent)
        }

        btnViewMyRequests.setOnClickListener {
            animateButtonPressOptimized(it)
            val intent = Intent(this, MyRequestsActivity::class.java)
            intent.putExtra("uid", auth.currentUser?.uid)
            startActivity(intent)
        }
    }

    private fun setupSearchFunctionality() {
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
    }

    private fun setupOptimizedScrollListener() {
        listView.setOnScrollListener(object : android.widget.AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: android.widget.AbsListView?, scrollState: Int) {}

            override fun onScroll(
                view: android.widget.AbsListView?,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {
                val threshold = 5
                if (!isLoadingPage && hasMoreData &&
                    (firstVisibleItem + visibleItemCount + threshold >= totalItemCount) &&
                    totalItemCount > 0) {
                    loadMoreItems()
                }
            }
        })
    }

    private fun setupNavigation() {
        navChats.setOnClickListener {
            animateNavItemOptimized(it)
            navigateWithUserData(ChatsActivity::class.java)
        }
        navProfile.setOnClickListener {
            animateNavItemOptimized(it)
            navigateWithUserData(ProfileActivity::class.java)
        }
        navMap.setOnClickListener {
            animateNavItemOptimized(it)
            navigateWithUserData(MapsActivity::class.java)
        }
        navNotifications.setOnClickListener {
            animateNavItemOptimized(it)
            navigateWithUserData(NotificationActivity::class.java)
        }
    }

    private fun animateButtonPressOptimized(view: View) {
        val scaleXDown = ObjectAnimator.ofFloat(view, "scaleX", 0.97f).apply {
            duration = 75
        }
        val scaleYDown = ObjectAnimator.ofFloat(view, "scaleY", 0.97f).apply {
            duration = 75
        }
        val scaleXUp = ObjectAnimator.ofFloat(view, "scaleX", 1.0f).apply {
            duration = 75
            startDelay = 75
        }
        val scaleYUp = ObjectAnimator.ofFloat(view, "scaleY", 1.0f).apply {
            duration = 75
            startDelay = 75
        }
        scaleXDown.start()
        scaleYDown.start()
        scaleXUp.start()
        scaleYUp.start()
    }

    private fun animateNavItemOptimized(view: View) {
        ObjectAnimator.ofFloat(view, "alpha", 0.8f).apply {
            duration = 100
            start()
        }
        view.postDelayed({
            ObjectAnimator.ofFloat(view, "alpha", 1.0f).apply {
                duration = 100
                start()
            }
        }, 100)
    }

    private fun navigateWithUserData(destinationClass: Class<*>) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    val role = document.getString("role") ?: ""
                    val intent = Intent(this, destinationClass).apply {
                        putExtra("uid", uid)
                        putExtra("role", role)
                    }
                    startActivity(intent)
                }
                .addOnFailureListener {
                    showStyledToast("Failed to fetch user data")
                }
        } else {
            showStyledToast("User not logged in")
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
            showStyledToast("User not logged in")
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
                showStyledToast("Failed to get user role")
            }
    }

    private fun fetchAllRequests() {
        firestore.collection("requests").get()
            .addOnSuccessListener { result ->
                allRequests = result.map { doc ->
                    JobRequest(
                        personName = doc.getString("userName") ?: "",
                        jobTitle = doc.getString("jobName") ?: "",
                        jobType = doc.getString("jobType") ?: "",
                        salary = doc.getString("price") ?: "",
                        createdByUid = doc.getString("uid") ?: "",
                        requestId = doc.id
                    )
                }
                filteredRequests = allRequests
                resetPagination()
                loadInitialItems()
            }
            .addOnFailureListener {
                showStyledToast("Failed to load requests")
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
        resetPagination()
        loadInitialItems()
    }

    private fun resetPagination() {
        currentPage = 0
        displayedRequests.clear()
        hasMoreData = true
        adapter = null
    }

    private fun loadInitialItems() {
        loadMoreItems()
    }

    private fun loadMoreItems() {
        if (isLoadingPage || !hasMoreData) return

        isLoadingPage = true

        val fromIndex = currentPage * itemsPerPage
        val toIndex = minOf(fromIndex + itemsPerPage, filteredRequests.size)

        if (fromIndex >= filteredRequests.size) {
            hasMoreData = false
            isLoadingPage = false
            return
        }

        val newItems = filteredRequests.subList(fromIndex, toIndex)
        displayedRequests.addAll(newItems)
        hasMoreData = toIndex < filteredRequests.size

        if (adapter == null) {
            adapter = JobRequestAdapter(this, displayedRequests)
            listView.adapter = adapter
            listView.setOnItemClickListener { _, _, position, _ ->
                if (position < displayedRequests.size) {
                    val selected = displayedRequests[position]
                    val intent = Intent(this, RequestDetailsActivity::class.java).apply {
                        putExtra("currentUid", auth.currentUser?.uid)
                        putExtra("creatorUid", selected.createdByUid)
                        putExtra("requestId", selected.requestId)
                    }
                    startActivity(intent)
                }
            }
        } else {
            adapter?.notifyDataSetChanged()
        }

        currentPage++
        isLoadingPage = false
    }

    private fun showStyledToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
