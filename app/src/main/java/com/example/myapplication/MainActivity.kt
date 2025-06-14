package com.example.myapplication

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.cardview.widget.CardView
import com.example.myapplication.model.JobRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var btnCreateRequest: Button
    private lateinit var btnViewMyRequests: Button
    private lateinit var btnFollowing: TextView
    private lateinit var followingCardView: CardView
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

    // Location variables
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0
    private val LOCATION_PERMISSION_REQUEST = 1001
    private val NEARBY_RADIUS_KM = 50.0 // 50km radius for nearby jobs

    // Filter states
    private var isShowingFollowing = false
    private var isShowingNearby = false
    private var appliedJobIds = mutableSetOf<String>()

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

        requestLocationPermission()
        checkUserRole()
        setupClickListeners()
        setupSearchFunctionality()
        setupOptimizedScrollListener()
        setupNavigation()
        fetchAppliedJobs()
    }

    private fun initializeViews() {
        btnCreateRequest = findViewById(R.id.btnNewRequest)
        btnViewMyRequests = findViewById(R.id.btnMyRequests)
        listView = findViewById(R.id.listView)
        searchBar = findViewById(R.id.searchEditText)

        // Get the following button properly
        val topButtonsLayout = findViewById<LinearLayout>(R.id.topButtonsLayout)
        followingCardView = topButtonsLayout.getChildAt(2) as CardView
        btnFollowing = followingCardView.findViewById<TextView>(android.R.id.text1)

        navHome = findViewById(R.id.navHome)
        navMap = findViewById(R.id.navMap)
        navNotifications = findViewById(R.id.navNotifications)
        navChats = findViewById(R.id.navChats)
        navProfile = findViewById(R.id.navProfile)

        // Make the following button clickable
        followingCardView.setOnClickListener {
            toggleFollowingView()
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        } else {
            getCurrentLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                showStyledToast("Location permission denied. Nearby jobs feature unavailable.")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try {
            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (lastKnownLocation != null) {
                userLatitude = lastKnownLocation.latitude
                userLongitude = lastKnownLocation.longitude

                // Enable location-based filtering
                setupLocationBasedFiltering()
            } else {
                // Request location update
                locationManager.requestSingleUpdate(
                    LocationManager.NETWORK_PROVIDER,
                    { location ->
                        userLatitude = location.latitude
                        userLongitude = location.longitude
                        setupLocationBasedFiltering()
                    },
                    null
                )
            }
        } catch (e: SecurityException) {
            showStyledToast("Unable to get location")
        }
    }

    private fun setupLocationBasedFiltering() {
        // Add click listener to "Jobs Near You" section
        findViewById<LinearLayout>(R.id.skillsSection).let { skillsSection ->
            // Find the "Jobs Near You" title and make it clickable
            val jobsNearYouTitle = skillsSection.getChildAt(skillsSection.childCount - 1) as LinearLayout
            jobsNearYouTitle.setOnClickListener {
                toggleNearbyView()
            }
        }
    }

    private fun toggleFollowingView() {
        isShowingFollowing = !isShowingFollowing
        isShowingNearby = false

        // Update button appearance
        updateButtonStates()

        if (isShowingFollowing) {
            showAppliedJobs()
        } else {
            showAllJobs()
        }
    }

    private fun toggleNearbyView() {
        if (userLatitude == 0.0 && userLongitude == 0.0) {
            showStyledToast("Location not available. Please enable location services.")
            return
        }

        isShowingNearby = !isShowingNearby
        isShowingFollowing = false

        updateButtonStates()

        if (isShowingNearby) {
            showNearbyJobs()
        } else {
            showAllJobs()
        }
    }

    private fun updateButtonStates() {
        // Update Following button appearance
        if (isShowingFollowing) {
            btnFollowing.text = "Following ✓"
            followingCardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary_button))
        } else {
            btnFollowing.text = "Following"
            followingCardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.tertiary_button))
        }

        // Update search hint based on current view
        when {
            isShowingFollowing -> searchBar.hint = "Search applied jobs..."
            isShowingNearby -> searchBar.hint = "Search nearby jobs..."
            else -> searchBar.hint = "Search for opportunities..."
        }
    }

    private fun fetchAppliedJobs() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("applications")
            .whereEqualTo("applicantUid", uid)
            .get()
            .addOnSuccessListener { result ->
                appliedJobIds.clear()
                for (document in result) {
                    document.getString("requestId")?.let { requestId ->
                        appliedJobIds.add(requestId)
                    }
                }
            }
            .addOnFailureListener {
                showStyledToast("Failed to load applied jobs")
            }
    }

    private fun showAppliedJobs() {
        if (appliedJobIds.isEmpty()) {
            displayedRequests.clear()
            adapter?.notifyDataSetChanged()
            showStyledToast("You haven't applied to any jobs yet")
            return
        }

        filteredRequests = allRequests.filter { job ->
            appliedJobIds.contains(job.requestId)
        }

        resetPagination()
        loadInitialItems()

        if (filteredRequests.isEmpty()) {
            showStyledToast("No applied jobs found")
        }
    }

    private fun showNearbyJobs() {
        if (userLatitude == 0.0 && userLongitude == 0.0) {
            showStyledToast("Location not available")
            return
        }

        // Filter jobs based on location (assuming jobs have latitude and longitude fields)
        filteredRequests = allRequests.filter { job ->
            // For demo purposes, generate random coordinates around user location
            // In real app, jobs should have actual latitude/longitude stored
            val jobLat = userLatitude + (Math.random() - 0.5) * 0.5 // Random within ~50km
            val jobLng = userLongitude + (Math.random() - 0.5) * 0.5

            val distance = calculateDistance(userLatitude, userLongitude, jobLat, jobLng)
            distance <= NEARBY_RADIUS_KM
        }

        // Sort by distance (closest first)
        filteredRequests = filteredRequests.sortedBy { job ->
            // Generate consistent random location for each job based on job ID
            val jobLat = userLatitude + (job.requestId.hashCode() % 100) * 0.001
            val jobLng = userLongitude + (job.requestId.hashCode() % 100) * 0.001
            calculateDistance(userLatitude, userLongitude, jobLat, jobLng)
        }

        resetPagination()
        loadInitialItems()

        if (filteredRequests.isEmpty()) {
            showStyledToast("No jobs found nearby")
        } else {
            showStyledToast("Found ${filteredRequests.size} jobs within ${NEARBY_RADIUS_KM.toInt()}km")
        }
    }

    private fun showAllJobs() {
        filteredRequests = allRequests
        resetPagination()
        loadInitialItems()
    }

    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers

        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    @SuppressLint("ObjectAnimatorBinding")
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

        // Determine the base list to search from
        val baseList = when {
            isShowingFollowing -> allRequests.filter { appliedJobIds.contains(it.requestId) }
            isShowingNearby -> {
                // Filter by location first
                allRequests.filter { job ->
                    val jobLat = userLatitude + (job.requestId.hashCode() % 100) * 0.001
                    val jobLng = userLongitude + (job.requestId.hashCode() % 100) * 0.001
                    calculateDistance(userLatitude, userLongitude, jobLat, jobLng) <= NEARBY_RADIUS_KM
                }
            }
            else -> allRequests
        }

        filteredRequests = if (lowerQuery.isEmpty()) {
            baseList
        } else {
            baseList.filter { item ->
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

    override fun onResume() {
        super.onResume()
        // Refresh applied jobs when returning to the activity
        fetchAppliedJobs()
    }
}