package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

class RequestDetailsActivity : AppCompatActivity() {

    private lateinit var tvUserName: TextView
    private lateinit var tvJobName: TextView
    private lateinit var tvJobType: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvSkill: TextView
    private lateinit var tvStartTime: TextView
    private lateinit var tvHours: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvExperience: TextView

    private lateinit var btnLocation: Button
    private lateinit var btnApply: Button
    private lateinit var btnViewProfile: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var currentUid: String
    private lateinit var creatorUid: String
    private lateinit var requestId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_details)

        tvUserName     = findViewById(R.id.tvUserName)
        tvJobName      = findViewById(R.id.tvJobName)
        tvJobType      = findViewById(R.id.tvJobType)
        tvPrice        = findViewById(R.id.tvPrice)
        tvSkill        = findViewById(R.id.tvSkill)
        tvStartTime    = findViewById(R.id.tvStartTime)
        tvHours        = findViewById(R.id.tvHours)
        tvLocation     = findViewById(R.id.tvLocation)
        tvExperience   = findViewById(R.id.tvExperience)

        btnLocation    = findViewById(R.id.btnLocation)
        btnApply       = findViewById(R.id.btnApply)
        btnViewProfile = findViewById(R.id.btnViewProfile)

        currentUid = intent.getStringExtra("currentUid") ?: ""
        creatorUid = intent.getStringExtra("creatorUid") ?: ""
        requestId  = intent.getStringExtra("requestId") ?: ""

        if (currentUid == creatorUid) {
            btnViewProfile.visibility = View.GONE
        }

        loadRequestData()
        checkRoleAndSetupButton()

        btnViewProfile.setOnClickListener {
            val intent = Intent(this, UserDetailsActivity::class.java)
            intent.putExtra("currentUserId", currentUid)
            intent.putExtra("userId", creatorUid)  // هنا المفتاح يجب أن يتطابق مع ما يستخدمه UserDetailsActivity
            startActivity(intent)
        }
    }

    private fun loadRequestData() {
        db.collection("requests").document(requestId).get()
            .addOnSuccessListener { doc ->
                tvUserName.text   = "${doc.getString("userName")}"
                tvJobName.text    = "Work Name: ${doc.getString("jobName")}"
                tvJobType.text    = "Work Type: ${doc.getString("jobType")}"
                tvPrice.text      = "Salary: ${doc.getString("price")}"
                tvSkill.text      = "Skill: ${doc.getString("skill")}"
                tvStartTime.text  = "Time Start: ${doc.getString("startTime")}"
                tvHours.text      = "Working Hours: ${doc.getString("hours")}"
                tvLocation.text   = "Location: ${doc.getString("location")} "
                tvExperience.text = "Experience: ${doc.getString("experience")}"

                val geo: GeoPoint? = doc.getGeoPoint("locationGeo")
                val locationName = doc.getString("location") ?: "Work Location"

                btnLocation.setOnClickListener {
                    if (geo != null) {
                        // Open MapsActivity with location coordinates
                        val intent = Intent(this, MapsActivity::class.java)
                        intent.putExtra("latitude", geo.latitude)
                        intent.putExtra("longitude", geo.longitude)
                        intent.putExtra("locationName", locationName)
                        intent.putExtra("showSpecificLocation", true)
                        // Pass request details for potential navigation back
                        intent.putExtra("requestId", requestId)
                        intent.putExtra("creatorUid", creatorUid)
                        intent.putExtra("currentUid", currentUid)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "لا توجد إحداثيات للموقع", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun checkRoleAndSetupButton() {
        db.collection("users").document(currentUid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role")
                if (role == "employee" && currentUid != creatorUid) {
                    btnApply.visibility = View.VISIBLE
                    btnApply.setOnClickListener {
                        sendApplication()
                    }
                }
            }
    }

    private fun sendApplication() {
        val appId = "${currentUid}_${creatorUid}_$requestId"
        val appRef = db.collection("app").document(appId)
        appRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val appData = hashMapOf(
                    "idee" to currentUid,
                    "ider" to creatorUid,
                    "requestId" to requestId
                )
                appRef.set(appData)

                // ✅ إشعار التقدم
                val notificationApply = hashMapOf(
                    "fromId" to currentUid,
                    "toId" to creatorUid,
                    "message" to "applied for",
                    "timestamp" to System.currentTimeMillis()
                )
                db.collection("notifications").add(notificationApply)

                val chatId = if (currentUid < creatorUid)
                    "${currentUid}_$creatorUid" else "${creatorUid}_$currentUid"

                val chatRef = db.collection("chats").document(chatId)
                chatRef.get().addOnSuccessListener { chatSnap ->
                    if (!chatSnap.exists()) {
                        val chatData = hashMapOf(
                            "user1" to currentUid,
                            "user2" to creatorUid
                        )
                        chatRef.set(chatData)

                        val notificationChat = hashMapOf(
                            "fromId" to currentUid,
                            "toId" to creatorUid,
                            "message" to "conversation started ",
                            "timestamp" to System.currentTimeMillis()
                        )
                        db.collection("notifications").add(notificationChat)
                    }
                }

                Toast.makeText(this, "تم إرسال الطلب بنجاح", Toast.LENGTH_SHORT).show()
                btnApply.visibility = View.GONE
            } else {
                Toast.makeText(this, "لقد أرسلت طلبًا بالفعل", Toast.LENGTH_SHORT).show()
            }
        }
    }
}