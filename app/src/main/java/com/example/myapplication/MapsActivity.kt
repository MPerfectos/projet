package com.example.myapplication

import android.Manifest
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
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import kotlin.random.Random


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

    // Bottom sheet views
    private lateinit var restaurantImage: ImageView
    private lateinit var restaurantName: TextView
    private lateinit var priceInfo: TextView
    private lateinit var checkButton: Button

    private val locationDataList = mutableListOf<LocationData>()
    private val markers = mutableListOf<Marker>()
    private var currentFilter = "All"
    private var currentSort = "Distance"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osm", MODE_PRIVATE))
        setContentView(R.layout.activity_maps)

        initializeViews()
        setupMap()
        setupBottomSheet()
        setupSearchFunctionality()
        setupFilterAndSort()
        generateSampleData()
        displayLocations()
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
        map.controller.setCenter(GeoPoint(36.75, 3.05)) // Algiers default

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        val eventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                // Handle map tap - could add new location or dismiss bottom sheet
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                showMarker(p)
                return true
            }
        }

        val overlayEvents = MapEventsOverlay(eventsReceiver)
        map.overlays.add(overlayEvents)
    }

    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.peekHeight = 200
        bottomSheetBehavior.isHideable = false
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        checkButton.setOnClickListener {
            Toast.makeText(this, "Checking ${restaurantName.text}...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSearchFunctionality() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterLocations(s.toString())
            }
        })

        findViewById<ImageView>(R.id.editIcon).setOnClickListener {
            // Handle edit action
            Toast.makeText(this, "Edit location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFilterAndSort() {
        filterButton.setOnClickListener {
            showFilterDialog()
        }

        sortButton.setOnClickListener {
            showSortDialog()
        }
    }

    private fun showFilterDialog() {
        val filters = arrayOf("All", "Restaurants", "Hotels", "Shops", "Services")
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
        val sortOptions = arrayOf("Distance", "Rating", "Price", "Name")
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
        clearMarkers()

        for (location in locationDataList) {
            val marker = Marker(map)
            marker.position = location.geoPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = location.name
            marker.snippet = "${location.price}${location.currency}/h"

            // Create custom info window
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

        // Update rating and distance info
        val ratingText = "${location.rating} (${location.reviews} reviews)"
        val distanceText = "${location.distance} miles"

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun filterLocations(searchQuery: String = "") {
        val filtered = locationDataList.filter { location ->
            val matchesSearch = searchQuery.isEmpty() ||
                    location.name.contains(searchQuery, ignoreCase = true)
            val matchesFilter = currentFilter == "All" ||
                    location.name.contains(getFilterKeyword(currentFilter), ignoreCase = true)
            matchesSearch && matchesFilter
        }

        displayFilteredLocations(filtered)
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
        val sorted = when (sortBy) {
            "Distance" -> locationDataList.sortedBy { it.distance }
            "Rating" -> locationDataList.sortedByDescending { it.rating }
            "Price" -> locationDataList.sortedBy { it.price }
            "Name" -> locationDataList.sortedBy { it.name }
            else -> locationDataList
        }

        locationDataList.clear()
        locationDataList.addAll(sorted)
        displayLocations()
    }

    private fun displayFilteredLocations(locations: List<LocationData>) {
        clearMarkers()

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

        resultsCounter.text = "${locations.size} results"
        map.invalidate()
    }

    private fun clearMarkers() {
        for (marker in markers) {
            map.overlays.remove(marker)
        }
        markers.clear()
    }

    private fun updateResultsCounter() {
        resultsCounter.text = "${locationDataList.size} results"
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
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}