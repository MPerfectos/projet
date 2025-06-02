package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
    private lateinit var btnApply: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var currentUid: String
    private lateinit var creatorUid: String
    private lateinit var requestId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_details)

        tvUserName = findViewById(R.id.tvUserName)
        tvJobName = findViewById(R.id.tvJobName)
        tvJobType = findViewById(R.id.tvJobType)
        tvPrice = findViewById(R.id.tvPrice)
        tvSkill = findViewById(R.id.tvSkill)
        tvStartTime = findViewById(R.id.tvStartTime)
        tvHours = findViewById(R.id.tvHours)
        tvLocation = findViewById(R.id.tvLocation)
        tvExperience = findViewById(R.id.tvExperience)
        btnApply = findViewById(R.id.btnApply)

        currentUid = intent.getStringExtra("currentUid") ?: ""
        creatorUid = intent.getStringExtra("creatorUid") ?: ""
        requestId = intent.getStringExtra("requestId") ?: ""

        loadRequestData()
        checkRoleAndSetupButton()
    }

    private fun loadRequestData() {
        db.collection("requests").document(requestId).get()
            .addOnSuccessListener { doc ->
                tvUserName.text = "${doc.getString("userName")}"
                tvJobName.text = "Work Name: ${doc.getString("jobName")}"
                tvJobType.text = "Work Type: ${doc.getString("jobType")}"
                tvPrice.text = "Salary: ${doc.getString("price")}"
                tvSkill.text = "Skill: ${doc.getString("skill")}"
                tvStartTime.text = "Time Start: ${doc.getString("startTime")}"
                tvHours.text = " Working Hours: ${doc.getString("hours")}"
                tvLocation.text = " Location: ${doc.getString("location")} "
                tvExperience.text = " Experience: ${doc.getString("experience")}"
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

                // إنشاء دردشة إذا لم تكن موجودة
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
