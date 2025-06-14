package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.myapplication.model.LocationData
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import kotlin.random.Random

// Data class لتمثيل طلبات العمل
data class JobRequest(
    val requestId: String = "",
    val jobName: String = "",
    val price: String = "",
    val location: String = "",
    val userName: String = "",
    val uid: String = "",
    val locationGeo: com.google.firebase.firestore.GeoPoint? = null,
    val hours: String = "",
    val skill: String = "",
    val experience: String = "",
    val jobType: String = "",
    val startTime: String = "",
    val isActive: Boolean = true
)

class MapsActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private lateinit var locationText: TextView
    private lateinit var searchEditText: EditText
    private lateinit var searchSubtext: TextView
    private lateinit var filterButton: Button
    private lateinit var sortButton: Button
    private lateinit var resultsCounter: TextView
    private lateinit var bottomSheet: LinearLayout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var db: FirebaseFirestore

    // Bottom sheet views
    private lateinit var restaurantImage: ImageView
    private lateinit var restaurantName: TextView
    private lateinit var priceInfo: TextView
    private lateinit var checkButton: Button

    private val locationDataList = mutableListOf<LocationData>()
    private val jobRequestsList = mutableListOf<JobRequest>()
    private val markers = mutableListOf<Marker>()
    private var currentFilter = "All"
    private var currentSort = "Distance"

    // Variables for specific location display
    private var specificLatitude: Double? = null
    private var specificLongitude: Double? = null
    private var specificLocationName: String? = null
    private var showSpecificLocation = false

    // Variables for job request details
    private var jobName: String? = null
    private var jobPrice: String? = null
    private var jobUserName: String? = null
    private var showJobCreated = false

    // Variables for request details navigation
    private var requestId: String? = null
    private var creatorUid: String? = null
    private var currentUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osm", MODE_PRIVATE))
        setContentView(R.layout.activity_maps)

        db = FirebaseFirestore.getInstance()

        // Get intent extras for specific location
        specificLatitude = intent.getDoubleExtra("latitude", 0.0).takeIf { it != 0.0 }
        specificLongitude = intent.getDoubleExtra("longitude", 0.0).takeIf { it != 0.0 }
        specificLocationName = intent.getStringExtra("locationName")
        showSpecificLocation = intent.getBooleanExtra("showSpecificLocation", false)

        // Get job details from intent
        jobName = intent.getStringExtra("jobName")
        jobPrice = intent.getStringExtra("price")
        jobUserName = intent.getStringExtra("userName")
        showJobCreated = intent.getBooleanExtra("showJobCreated", false)

        // Get request details for potential navigation back
        requestId = intent.getStringExtra("requestId")
        creatorUid = intent.getStringExtra("creatorUid")
        currentUid = intent.getStringExtra("currentUid")

        initializeViews()
        setupMap()
        setupBottomSheet()
        setupSearchFunctionality()
        setupFilterAndSort()

        if (showSpecificLocation && specificLatitude != null && specificLongitude != null) {
            // Show specific location from intent
            showSpecificLocationOnMap()
        } else {
            // Load job requests from Firebase and show normal map
            loadJobRequestsFromFirebase()
            generateSampleData()
            displayLocations()
        }
    }

    private fun initializeViews() {
        map = findViewById(R.id.map)
        locationText = findViewById(R.id.locationText)
        searchEditText = findViewById(R.id.searchEditText)
        searchSubtext = findViewById(R.id.searchSubtext)
        filterButton = findViewById(R.id.filterButton)
        sortButton = findViewById(R.id.sortButton)
        resultsCounter = findViewById(R.id.resultsCounter)
        bottomSheet = findViewById(R.id.bottomSheet)

        // Bottom sheet views
        restaurantImage = findViewById(R.id.restaurantImage)
        restaurantName = findViewById(R.id.restaurantName)
        priceInfo = findViewById(R.id.priceInfo)
        checkButton = findViewById(R.id.checkButton)
    }

    private fun setupMap() {
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)

        // Set center based on whether we have specific coordinates or use default
        val centerPoint = if (specificLatitude != null && specificLongitude != null) {
            GeoPoint(specificLatitude!!, specificLongitude!!)
        } else {
            GeoPoint(36.75, 3.05) // Algiers default
        }
        map.controller.setCenter(centerPoint)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        val eventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                // Handle map tap - could add new location or dismiss bottom sheet
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                if (!showSpecificLocation) {
                    showMarker(p)
                }
                return true
            }
        }

        val overlayEvents = MapEventsOverlay(eventsReceiver)
        map.overlays.add(overlayEvents)
    }

    private fun loadJobRequestsFromFirebase() {
        db.collection("requests")
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { documents ->
                jobRequestsList.clear()
                for (document in documents) {
                    try {
                        val jobRequest = JobRequest(
                            requestId = document.id,
                            jobName = document.getString("jobName") ?: "",
                            price = document.getString("price") ?: "",
                            location = document.getString("location") ?: "",
                            userName = document.getString("userName") ?: "",
                            uid = document.getString("uid") ?: "",
                            locationGeo = document.getGeoPoint("locationGeo"),
                            hours = document.getString("hours") ?: "",
                            skill = document.getString("skill") ?: "",
                            experience = document.getString("experience") ?: "",
                            jobType = document.getString("jobType") ?: "",
                            startTime = document.getString("startTime") ?: "",
                            isActive = document.getBoolean("isActive") ?: true
                        )
                        if (jobRequest.locationGeo != null) {
                            jobRequestsList.add(jobRequest)
                        }
                    } catch (e: Exception) {
                        // Skip invalid documents
                    }
                }
                displayJobRequests()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "خطأ في تحميل طلبات العمل: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayJobRequests() {
        // Clear existing job markers
        val jobMarkers = markers.filter { it.title?.contains("Job:") == true }
        jobMarkers.forEach { marker ->
            map.overlays.remove(marker)
            markers.remove(marker)
        }

        // Add job request markers
        for (jobRequest in jobRequestsList) {
            jobRequest.locationGeo?.let { geoPoint ->
                val marker = Marker(map)
                marker.position = GeoPoint(geoPoint.latitude, geoPoint.longitude)
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = "Job: ${jobRequest.jobName}"
                marker.snippet = "${jobRequest.price} DZD/h - ${jobRequest.userName}"

                // Different marker styling for job requests
                // You can customize the marker appearance here
                marker.setOnMarkerClickListener { clickedMarker, _ ->
                    showJobRequestDetails(jobRequest)
                    true
                }

                map.overlays.add(marker)
                markers.add(marker)
            }
        }

        updateResultsCounter()
        map.invalidate()
    }

    private fun showJobRequestDetails(jobRequest: JobRequest) {
        restaurantName.text = jobRequest.jobName
        priceInfo.text = "${jobRequest.price} DZD/h"

        // Show additional job details
        val additionalInfo = buildString {
            append("Employer: ${jobRequest.userName}\n")
            if (jobRequest.jobType.isNotEmpty()) {
                append("Type: ${jobRequest.jobType}\n")
            }
            if (jobRequest.hours.isNotEmpty()) {
                append("Hours: ${jobRequest.hours}\n")
            }
            if (jobRequest.skill.isNotEmpty()) {
                append("Required Skill: ${jobRequest.skill}\n")
            }
            if (jobRequest.experience.isNotEmpty()) {
                append("Experience: ${jobRequest.experience}\n")
            }
            if (jobRequest.startTime.isNotEmpty()) {
                append("Start Time: ${jobRequest.startTime}")
            }
        }

        // You can show this additional info in a TextView if available in your layout
        // For now, we'll use the existing priceInfo to show some details
        priceInfo.text = "${jobRequest.price} DZD/h\nBy: ${jobRequest.userName}"

        checkButton.text = "View Job Details"
        checkButton.setOnClickListener {
            // Navigate to job details if RequestDetailsActivity exists
            // Otherwise show a toast with job information
            val intent = Intent(this, RequestDetailsActivity::class.java)
            intent.putExtra("requestId", jobRequest.requestId)
            intent.putExtra("creatorUid", jobRequest.uid)
            intent.putExtra("currentUid", currentUid ?: "")

            try {
                startActivity(intent)
            } catch (e: Exception) {
                // If RequestDetailsActivity doesn't exist, show details in toast
                Toast.makeText(this, additionalInfo, Toast.LENGTH_LONG).show()
            }
        }

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun showSpecificLocationOnMap() {
        if (specificLatitude != null && specificLongitude != null) {
            val geoPoint = GeoPoint(specificLatitude!!, specificLongitude!!)

            // Create marker for the specific location
            val marker = Marker(map)
            marker.position = geoPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            // Use job details if available
            if (showJobCreated && jobName != null && jobPrice != null) {
                marker.title = "Job: $jobName"
                marker.snippet = "$jobPrice DZD/h - ${jobUserName ?: "Unknown"}"
            } else {
                marker.title = specificLocationName ?: "Work Location"
                marker.snippet = "Job location"
            }

            marker.setOnMarkerClickListener { clickedMarker, _ ->
                showWorkLocationDetails()
                true
            }

            map.overlays.add(marker)
            markers.add(marker)

            // Update location text
            if (showJobCreated && jobName != null) {
                locationText.text = "Job Created: $jobName"
                if (jobPrice != null) {
                    locationText.text = "${locationText.text} - $jobPrice DZD/h"
                }
            } else {
                locationText.text = "Work Location: ${specificLocationName ?: "Selected Location"}"
            }
            locationText.visibility = View.VISIBLE

            // Update results counter
            resultsCounter.text = "1 job location"

            // Hide filter and sort buttons for specific location view
            filterButton.visibility = View.GONE
            sortButton.visibility = View.GONE
            searchEditText.visibility = View.GONE
            searchSubtext.visibility = View.GONE

            // Set higher zoom for specific location
            map.controller.setZoom(17.0)
            map.controller.setCenter(geoPoint)

            map.invalidate()
        }
    }

    private fun showWorkLocationDetails() {
        if (showJobCreated && jobName != null && jobPrice != null) {
            restaurantName.text = jobName
            priceInfo.text = "$jobPrice DZD/h\nBy: ${jobUserName ?: "Unknown"}"

            if (showJobCreated) {
                checkButton.text = "Job Created Successfully!"
                checkButton.setOnClickListener {
                    Toast.makeText(this, "Your job has been posted successfully!", Toast.LENGTH_SHORT).show()
                    finish() // Return to previous activity
                }
            }
        } else {
            restaurantName.text = specificLocationName ?: "Work Location"
            priceInfo.text = "Job Location"

            checkButton.text = "View Job Details"
            checkButton.setOnClickListener {
                if (requestId != null && creatorUid != null && currentUid != null) {
                    val intent = Intent(this, RequestDetailsActivity::class.java)
                    intent.putExtra("requestId", requestId)
                    intent.putExtra("creatorUid", creatorUid)
                    intent.putExtra("currentUid", currentUid)

                    try {
                        startActivity(intent)
                        finish()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Job details not available", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Job details not available", Toast.LENGTH_SHORT).show()
                }
            }
        }

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.peekHeight = 200
        bottomSheetBehavior.isHideable = false
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        if (!showSpecificLocation) {
            checkButton.setOnClickListener {
                Toast.makeText(this, "Checking ${restaurantName.text}...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSearchFunctionality() {
        if (!showSpecificLocation) {
            searchEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    filterLocations(s.toString())
                }
            })

            findViewById<ImageView>(R.id.editIcon).setOnClickListener {
                Toast.makeText(this, "Edit location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupFilterAndSort() {
        if (!showSpecificLocation) {
            filterButton.setOnClickListener {
                showFilterDialog()
            }

            sortButton.setOnClickListener {
                showSortDialog()
            }
        }
    }

    private fun showFilterDialog() {
        val filters = arrayOf("All", "Jobs", "Restaurants", "Hotels", "Shops", "Services")
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Filter by category")
        builder.setItems(filters) { _, which ->
            currentFilter = filters[which]
            searchSubtext.text = currentFilter
            filterLocations()
        }
        builder.show()
    }

    private fun showSortDialog() {
        val sortOptions = arrayOf("Distance", "Price", "Name", "Recent")
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Sort by")
        builder.setItems(sortOptions) { _, which ->
            currentSort = sortOptions[which]
            sortLocations(currentSort)
        }
        builder.show()
    }

    private fun generateSampleData() {
        val sampleLocations = listOf(
            LocationData("Pizza Palermo", 4.8, 500, 1.2, 110, "DZD",
                GeoPoint(36.751, 3.051)),
            LocationData("Brahim Restaurant", 4.5, 320, 0.8, 120, "DZD",
                GeoPoint(36.752, 3.053)),
            LocationData("Cafe Central", 4.2, 150, 2.1, 80, "DZD",
                GeoPoint(36.749, 3.048)),
            LocationData("Hotel Mazafran", 4.6, 280, 1.5, 200, "DZD",
                GeoPoint(36.753, 3.052)),
            LocationData("Supermarket", 4.0, 95, 0.5, 100, "DZD",
                GeoPoint(36.748, 3.049)),
            LocationData("Pharmacy", 4.3, 67, 0.3, 50, "DZD",
                GeoPoint(36.754, 3.047))
        )

        locationDataList.addAll(sampleLocations)
    }

    private fun displayLocations() {
        clearNonJobMarkers()

        for (location in locationDataList) {
            val marker = Marker(map)
            marker.position = location.geoPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = location.name
            marker.snippet = "${location.price}${location.currency}/h"

            marker.setOnMarkerClickListener { clickedMarker, _ ->
                showLocationDetails(location)
                true
            }

            map.overlays.add(marker)
            markers.add(marker)
        }

        updateResultsCounter()
        map.invalidate()
    }

    private fun showLocationDetails(location: LocationData) {
        restaurantName.text = location.name
        priceInfo.text = "${location.price}${location.currency} / hour"

        checkButton.text = "Check"
        checkButton.setOnClickListener {
            Toast.makeText(this, "Checking ${restaurantName.text}...", Toast.LENGTH_SHORT).show()
        }

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun filterLocations(searchQuery: String = "") {
        if (showSpecificLocation) return

        val filteredLocations = locationDataList.filter { location ->
            val matchesSearch = searchQuery.isEmpty() ||
                    location.name.contains(searchQuery, ignoreCase = true)
            val matchesFilter = currentFilter == "All" || currentFilter == "Restaurants" ||
                    location.name.contains(getFilterKeyword(currentFilter), ignoreCase = true)
            matchesSearch && matchesFilter
        }

        val filteredJobs = jobRequestsList.filter { job ->
            val matchesSearch = searchQuery.isEmpty() ||
                    job.jobName.contains(searchQuery, ignoreCase = true) ||
                    job.userName.contains(searchQuery, ignoreCase = true)
            val matchesFilter = currentFilter == "All" || currentFilter == "Jobs"
            matchesSearch && matchesFilter
        }

        displayFilteredContent(filteredLocations, filteredJobs)
    }

    private fun getFilterKeyword(filter: String): String {
        return when (filter) {
            "Restaurants" -> "restaurant|pizza|cafe"
            "Hotels" -> "hotel"
            "Shops" -> "supermarket|shop"
            "Services" -> "pharmacy|service"
            else -> ""
        }
    }

    private fun sortLocations(sortBy: String) {
        if (showSpecificLocation) return

        val sortedLocations = when (sortBy) {
            "Distance" -> locationDataList.sortedBy { it.distance }
            "Price" -> locationDataList.sortedBy { it.price }
            "Name" -> locationDataList.sortedBy { it.name }
            else -> locationDataList
        }

        val sortedJobs = when (sortBy) {
            "Price" -> jobRequestsList.sortedBy { it.price.toIntOrNull() ?: 0 }
            "Name" -> jobRequestsList.sortedBy { it.jobName }
            "Recent" -> jobRequestsList.sortedByDescending { it.requestId } // Assuming newer requests have later IDs
            else -> jobRequestsList
        }

        locationDataList.clear()
        locationDataList.addAll(sortedLocations)
        jobRequestsList.clear()
        jobRequestsList.addAll(sortedJobs)

        displayLocations()
        displayJobRequests()
    }

    private fun displayFilteredContent(locations: List<LocationData>, jobs: List<JobRequest>) {
        clearMarkers()

        // Display filtered locations
        for (location in locations) {
            val marker = Marker(map)
            marker.position = location.geoPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = location.name
            marker.snippet = "${location.price}${location.currency}/h"

            marker.setOnMarkerClickListener { _, _ ->
                showLocationDetails(location)
                true
            }

            map.overlays.add(marker)
            markers.add(marker)
        }

        // Display filtered job requests
        for (jobRequest in jobs) {
            jobRequest.locationGeo?.let { geoPoint ->
                val marker = Marker(map)
                marker.position = GeoPoint(geoPoint.latitude, geoPoint.longitude)
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = "Job: ${jobRequest.jobName}"
                marker.snippet = "${jobRequest.price} DZD/h - ${jobRequest.userName}"

                marker.setOnMarkerClickListener { clickedMarker, _ ->
                    showJobRequestDetails(jobRequest)
                    true
                }

                map.overlays.add(marker)
                markers.add(marker)
            }
        }

        resultsCounter.text = "${locations.size + jobs.size} results"
        map.invalidate()
    }

    private fun clearMarkers() {
        for (marker in markers) {
            map.overlays.remove(marker)
        }
        markers.clear()
    }

    private fun clearNonJobMarkers() {
        val nonJobMarkers = markers.filter { it.title?.contains("Job:") != true }
        nonJobMarkers.forEach { marker ->
            map.overlays.remove(marker)
            markers.remove(marker)
        }
    }

    private fun updateResultsCounter() {
        val totalResults = locationDataList.size + jobRequestsList.size
        resultsCounter.text = "$totalResults results"
    }

    private fun showMarker(point: GeoPoint) {
        val marker = Marker(map)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "Selected Location"
        map.overlays.add(marker)
        locationText.text = "Lat: ${point.latitude}, Lon: ${point.longitude}"
        locationText.visibility = View.VISIBLE
        map.invalidate()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        // Reload job requests when returning to the activity
        if (!showSpecificLocation) {
            loadJobRequestsFromFirebase()
        }
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}