package com.example.myapplication

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.jvm.java
import androidx.core.content.edit

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // Drawer components
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerContainer: LinearLayout
    private lateinit var menuIcon: ImageView

    // Profile elements
    private lateinit var profileImage: ImageView
    private lateinit var profileName: TextView
    private lateinit var monthSelector: Button

    // Dashboard stats
    private lateinit var totalBalanceText: TextView
    private lateinit var balanceChangeText: TextView
    private lateinit var hoursWorkedText: TextView
    private lateinit var hoursChangeText: TextView

    // Chart placeholder
    private lateinit var chartContainer: LinearLayout

    // Recent jobs
    private lateinit var recentJobsList: ListView

    // Navigation
    private lateinit var navHome: LinearLayout
    private lateinit var navMap: LinearLayout
    private lateinit var navNotifications: LinearLayout
    private lateinit var navChats: LinearLayout
    private lateinit var navProfile: LinearLayout

    // Drawer menu items
    private lateinit var drawerLanguage: LinearLayout
    private lateinit var drawerEditProfile: LinearLayout
    private lateinit var drawerTheme: LinearLayout
    private lateinit var drawerSignOut: LinearLayout
    private lateinit var themeToggleText: TextView

    private var uid: String = ""
    private var userRole: String = ""
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

        // Apply saved theme before setting content view
        applySavedTheme()

        // Enable edge-to-edge and set status bar appearance
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = isDarkMode()
        }

        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Get user data from intent or current user
        uid = intent.getStringExtra("uid") ?: auth.currentUser?.uid ?: ""
        userRole = intent.getStringExtra("role") ?: ""

        Log.d("ProfileActivity", "UID: $uid, Role: $userRole")

        if (uid.isEmpty()) {
            showStyledToast("User not logged in")
            finish()
            return
        }

        if (!isConnected()) {
            showStyledToast("No internet connection available")
            // Don't return here, allow offline viewing with cached data
        }

        initializeViews()
        setupDrawer()
        setupClickListeners()
        setupNavigation()
        loadUserProfile()
        loadDashboardData()
        loadRecentJobs()
    }

    private fun initializeViews() {
        try {
            // Drawer components
            drawerLayout = findViewById(R.id.drawerLayout)
            drawerContainer = findViewById(R.id.drawerContainer)
            menuIcon = findViewById(R.id.menuIcon)

            profileImage = findViewById(R.id.profileImage)
            profileName = findViewById(R.id.profileName)
            monthSelector = findViewById(R.id.monthSelector)

            totalBalanceText = findViewById(R.id.totalBalanceText)
            balanceChangeText = findViewById(R.id.balanceChangeText)
            hoursWorkedText = findViewById(R.id.hoursWorkedText)
            hoursChangeText = findViewById(R.id.hoursChangeText)

            chartContainer = findViewById(R.id.chartContainer)
            recentJobsList = findViewById(R.id.recentJobsList)

            // Navigation
            navHome = findViewById(R.id.navHome)
            navMap = findViewById(R.id.navMap)
            navNotifications = findViewById(R.id.navNotifications)
            navChats = findViewById(R.id.navChats)
            navProfile = findViewById(R.id.navProfile)

            // Drawer menu items
            drawerLanguage = findViewById(R.id.drawerLanguage)
            drawerEditProfile = findViewById(R.id.drawerEditProfile)
            drawerTheme = findViewById(R.id.drawerTheme)
            drawerSignOut = findViewById(R.id.drawerSignOut)
            themeToggleText = findViewById(R.id.themeToggleText)

        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error initializing views", e)
            showStyledToast("Error loading profile interface")
            finish()
        }
    }

    private fun setupDrawer() {
        // Update theme toggle text based on current theme
        updateThemeToggleText()

        // Menu icon click listener
        menuIcon.setOnClickListener {
            animateButtonPress(it)
            if (drawerLayout.isDrawerOpen(drawerContainer)) {
                drawerLayout.closeDrawer(drawerContainer)
            } else {
                drawerLayout.openDrawer(drawerContainer)
            }
        }

        // Drawer menu item click listeners
        drawerLanguage.setOnClickListener {
            animateDrawerItem(it)
            showLanguageDialog()
        }

        drawerEditProfile.setOnClickListener {
            animateDrawerItem(it)
            showEditProfileDialog()
        }

        drawerTheme.setOnClickListener {
            animateDrawerItem(it)
            toggleTheme()
        }

        drawerSignOut.setOnClickListener {
            animateDrawerItem(it)
            showSignOutDialog()
        }
    }

    private fun setupClickListeners() {
        monthSelector.setOnClickListener {
            animateButtonPress(it)
            showMonthPicker()
        }

        profileImage.setOnClickListener {
            animateButtonPress(it)
            // TODO: Implement profile image selection
            showStyledToast("Profile image selection coming soon")
        }
    }

    private fun setupNavigation() {
        navHome.setOnClickListener {
            animateNavItem(it)
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("uid", uid)
                putExtra("role", userRole)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }

        navChats.setOnClickListener {
            animateNavItem(it)
            val intent = Intent(this, ChatsActivity::class.java).apply {
                putExtra("uid", uid)
                putExtra("role", userRole)
            }
            startActivity(intent)
        }

        navMap.setOnClickListener {
            animateNavItem(it)
            val intent = Intent(this, MapsActivity::class.java).apply {
                putExtra("uid", uid)
                putExtra("role", userRole)
            }
            startActivity(intent)
        }

        navNotifications.setOnClickListener {
            animateNavItem(it)
            val intent = Intent(this, NotificationActivity::class.java).apply {
                putExtra("uid", uid)
                putExtra("role", userRole)
            }
            startActivity(intent)
        }

        // Profile nav is current page, so just animate
        navProfile.setOnClickListener {
            animateNavItem(it)
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("English", "العربية", "Français")
        val currentLanguage = sharedPreferences.getString("language", "English")
        var selectedIndex = languages.indexOf(currentLanguage)
        if (selectedIndex == -1) selectedIndex = 0

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Language")
        builder.setSingleChoiceItems(languages, selectedIndex) { dialog, which ->
            val selectedLanguage = languages[which]
            sharedPreferences.edit().putString("language", selectedLanguage).apply()

            // Apply language change
            when (selectedLanguage) {
                "العربية" -> setLocale("ar")
                "Français" -> setLocale("fr")
                else -> setLocale("en")
            }

            showStyledToast("Language changed to $selectedLanguage")
            dialog.dismiss()
            drawerLayout.closeDrawer(drawerContainer)
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun showEditProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialogue_edit_profile, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.editProfileName)
        val emailEditText = dialogView.findViewById<EditText>(R.id.editProfileEmail)

        // Pre-fill current name
        nameEditText.setText(profileName.text.toString())

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Profile")
        builder.setView(dialogView)
        builder.setPositiveButton("Save") { dialog, _ ->
            val newName = nameEditText.text.toString().trim()
            val newEmail = emailEditText.text.toString().trim()

            if (newName.isNotEmpty()) {
                updateUserProfile(newName, newEmail)
            } else {
                showStyledToast("Name cannot be empty")
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
        drawerLayout.closeDrawer(drawerContainer)
    }

    private fun toggleTheme() {
        val isDark = isDarkMode()
        val newMode = if (isDark) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES

        // Save theme preference
        sharedPreferences.edit().putBoolean("dark_mode", !isDark).apply()

        // Apply theme
        AppCompatDelegate.setDefaultNightMode(newMode)

        showStyledToast("Theme changed to ${if (!isDark) "Dark" else "Light"} mode")
        drawerLayout.closeDrawer(drawerContainer)
    }

    private fun showSignOutDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Sign Out")
        builder.setMessage("Are you sure you want to sign out?")
        builder.setPositiveButton("Sign Out") { dialog, _ ->
            signOut()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
        drawerLayout.closeDrawer(drawerContainer)
    }

    private fun updateUserProfile(name: String, email: String) {
        val updates = mutableMapOf<String, Any>()
        updates["name"] = name
        if (email.isNotEmpty()) {
            updates["email"] = email
        }

        firestore.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener {
                profileName.text = name
                showStyledToast("Profile updated successfully")
                Log.d("ProfileActivity", "Profile updated: $name")
            }
            .addOnFailureListener { exception ->
                Log.e("ProfileActivity", "Error updating profile", exception)
                showStyledToast("Failed to update profile: ${exception.message}")
            }
    }

    private fun signOut() {
        try {
            auth.signOut()

            // Clear saved preferences
            sharedPreferences.edit { clear() }

            // Navigate to login/main activity
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error signing out", e)
            showStyledToast("Error signing out")
        }
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun applySavedTheme() {
        val isDark = sharedPreferences.getBoolean("dark_mode", false)
        val mode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun isDarkMode(): Boolean {
        return when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }
    }

    private fun updateThemeToggleText() {
        themeToggleText.text = if (isDarkMode()) "Switch to Light Mode" else "Switch to Dark Mode"
    }

    private fun loadUserProfile() {
        if (uid.isEmpty()) {
            showStyledToast("Invalid user ID")
            return
        }

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: "User"
                    val role = document.getString("role") ?: ""

                    profileName.text = name

                    // Update userRole if it wasn't passed in the intent
                    if (userRole.isEmpty()) {
                        userRole = role
                    }

                    Log.d("ProfileActivity", "Loaded profile: $name, role: $role")

                    // Set default profile image based on role
                    if (role == "employee") {
                        profileImage.setImageResource(R.drawable.default_avatar)
                    } else {
                        profileImage.setImageResource(R.drawable.default_avatar)
                    }
                } else {
                    Log.w("ProfileActivity", "User document does not exist")
                    showStyledToast("User profile not found")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ProfileActivity", "Error loading profile", exception)
                showStyledToast("Failed to load profile: ${exception.message}")
            }
    }

    private fun loadDashboardData() {
        val calendar = Calendar.getInstance()
        calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.time

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val monthEnd = calendar.time

        // Load current month data
        loadMonthlyEarnings(monthStart, monthEnd, true)

        // Load previous month data for comparison
        calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.MONTH, -1)
        val prevMonthStart = calendar.time

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val prevMonthEnd = calendar.time

        loadMonthlyEarnings(prevMonthStart, prevMonthEnd, false)

        updateMonthDisplay()
    }

    private fun loadMonthlyEarnings(startDate: Date, endDate: Date, isCurrentMonth: Boolean) {
        if (uid.isEmpty() || userRole.isEmpty()) {
            Log.w("ProfileActivity", "UID or role is empty, cannot load earnings")
            if (isCurrentMonth) {
                // Show default values
                totalBalanceText.text = "0.00 DZD"
                hoursWorkedText.text = "0"
            }
            return
        }

        val query = if (userRole == "employee") {
            firestore.collection("transactions")
                .whereEqualTo("employeeId", uid)
                .whereGreaterThanOrEqualTo("completedAt", startDate)
                .whereLessThanOrEqualTo("completedAt", endDate)
        } else {
            firestore.collection("transactions")
                .whereEqualTo("employerId", uid)
                .whereGreaterThanOrEqualTo("completedAt", startDate)
                .whereLessThanOrEqualTo("completedAt", endDate)
        }

        query.get()
            .addOnSuccessListener { result ->
                var totalEarnings = 0.0
                var totalHours = 0.0

                for (document in result) {
                    val amount = document.getDouble("amount") ?: 0.0
                    val hours = document.getDouble("hoursWorked") ?: 0.0

                    totalEarnings += amount
                    totalHours += hours
                }

                Log.d("ProfileActivity", "Loaded ${if (isCurrentMonth) "current" else "previous"} month: earnings=$totalEarnings, hours=$totalHours")

                if (isCurrentMonth) {
                    updateCurrentMonthDisplay(totalEarnings, totalHours)
                } else {
                    updatePreviousMonthComparison(totalEarnings, totalHours)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ProfileActivity", "Error loading monthly earnings", exception)
                if (isCurrentMonth) {
                    // Show default values
                    totalBalanceText.text = "0.00 DZD"
                    hoursWorkedText.text = "0"
                }
            }
    }

    private var currentMonthEarnings = 0.0
    private var currentMonthHours = 0.0
    private var previousMonthEarnings = 0.0
    private var previousMonthHours = 0.0

    private fun updateCurrentMonthDisplay(earnings: Double, hours: Double) {
        currentMonthEarnings = earnings
        currentMonthHours = hours

        val formatter = NumberFormat.getNumberInstance(Locale.getDefault())
        totalBalanceText.text = "${formatter.format(earnings)} DZD"
        hoursWorkedText.text = "${hours.toInt()}"
    }

    private fun updatePreviousMonthComparison(earnings: Double, hours: Double) {
        previousMonthEarnings = earnings
        previousMonthHours = hours

        // Calculate percentage changes
        val earningsChange = if (previousMonthEarnings > 0) {
            ((currentMonthEarnings - previousMonthEarnings) / previousMonthEarnings * 100).toInt()
        } else if (currentMonthEarnings > 0) {
            100
        } else {
            0
        }

        val hoursChange = if (previousMonthHours > 0) {
            ((currentMonthHours - previousMonthHours) / previousMonthHours * 100).toInt()
        } else if (currentMonthHours > 0) {
            100
        } else {
            0
        }

        // Update change displays
        balanceChangeText.text = "${if (earningsChange >= 0) "+" else ""}$earningsChange% last month"
        hoursChangeText.text = "${if (hoursChange >= 0) "+" else ""}$hoursChange% last month"

        // Set colors based on positive/negative change
        balanceChangeText.setTextColor(if (earningsChange >= 0)
            resources.getColor(android.R.color.holo_green_dark, null) else
            resources.getColor(android.R.color.holo_red_dark, null))

        hoursChangeText.setTextColor(if (hoursChange >= 0)
            resources.getColor(android.R.color.holo_green_dark, null) else
            resources.getColor(android.R.color.holo_red_dark, null))
    }

    private fun loadRecentJobs() {
        if (uid.isEmpty() || userRole.isEmpty()) {
            Log.w("ProfileActivity", "UID or role is empty, cannot load recent jobs")
            return
        }

        val query = if (userRole == "employee") {
            firestore.collection("transactions")
                .whereEqualTo("employeeId", uid)
                .orderBy("completedAt", Query.Direction.DESCENDING)
                .limit(5)
        } else {
            firestore.collection("requests")
                .whereEqualTo("uid", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(5)
        }

        query.get()
            .addOnSuccessListener { result ->
                val recentJobs = mutableListOf<Map<String, Any>>()

                for (document in result) {
                    val jobData = mutableMapOf<String, Any>()
                    jobData["id"] = document.id

                    if (userRole == "employee") {
                        jobData["title"] = document.getString("jobTitle") ?: "Unknown Job"
                        jobData["employer"] = document.getString("employerName") ?: "Unknown Employer"
                        jobData["amount"] = document.getDouble("amount") ?: 0.0
                    } else {
                        jobData["title"] = document.getString("jobName") ?: "Unknown Job"
                        jobData["status"] = document.getString("status") ?: "pending"
                        jobData["price"] = document.getString("price") ?: "0"
                    }

                    recentJobs.add(jobData)
                }

                Log.d("ProfileActivity", "Loaded ${recentJobs.size} recent jobs")

                val adapter = RecentJobsAdapter(this, recentJobs, userRole)
                recentJobsList.adapter = adapter
            }
            .addOnFailureListener { exception ->
                Log.e("ProfileActivity", "Error loading recent jobs", exception)
                showStyledToast("Failed to load recent jobs: ${exception.message}")
            }
    }

    private fun showMonthPicker() {
        val months = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Month")
        builder.setItems(months) { _, which ->
            currentMonth = which
            loadDashboardData()
        }
        builder.show()
    }

    private fun updateMonthDisplay() {
        val calendar = Calendar.getInstance()
        calendar.set(currentYear, currentMonth, 1)
        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
        monthSelector.text = monthName
    }

    private fun animateButtonPress(view: View) {
        ObjectAnimator.ofFloat(view, "scaleX", 0.95f).apply {
            duration = 100
            start()
        }
        ObjectAnimator.ofFloat(view, "scaleY", 0.95f).apply {
            duration = 100
            start()
        }

        view.postDelayed({
            ObjectAnimator.ofFloat(view, "scaleX", 1.0f).apply {
                duration = 100
                start()
            }
            ObjectAnimator.ofFloat(view, "scaleY", 1.0f).apply {
                duration = 100
                start()
            }
        }, 100)
    }

    private fun animateNavItem(view: View) {
        ObjectAnimator.ofFloat(view, "alpha", 0.7f).apply {
            duration = 150
            start()
        }

        view.postDelayed({
            ObjectAnimator.ofFloat(view, "alpha", 1.0f).apply {
                duration = 150
                start()
            }
        }, 150)
    }

    private fun animateDrawerItem(view: View) {
        ObjectAnimator.ofFloat(view, "alpha", 0.5f).apply {
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

    private fun isConnected(): Boolean {
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error checking connectivity", e)
            false
        }
    }

    private fun showStyledToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(drawerContainer)) {
            drawerLayout.closeDrawer(drawerContainer)
        } else {
            super.onBackPressed()
        }
    }
}