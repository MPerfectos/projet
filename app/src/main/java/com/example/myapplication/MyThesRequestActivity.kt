package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class MyThesRequestActivity : AppCompatActivity() {

    private lateinit var tvJobDetails: TextView
    private lateinit var btnDelete: Button
    private lateinit var listView: ListView
    private lateinit var tvNoApplicants: TextView

    private lateinit var db: FirebaseFirestore
    private lateinit var uid: String
    private lateinit var requestId: String

    private var applicants = mutableListOf<Pair<String, String>>() // uid, name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_thes_request)

        tvJobDetails = findViewById(R.id.tvJobDetails)
        btnDelete = findViewById(R.id.btnDeleteRequest)
        listView = findViewById(R.id.listViewApplicants)
        tvNoApplicants = findViewById(R.id.tvNoApplicants)

        uid = intent.getStringExtra("uid") ?: ""
        requestId = intent.getStringExtra("requestId") ?: ""
        db = FirebaseFirestore.getInstance()

        loadRequestDetails()
        loadApplicants()

        btnDelete.setOnClickListener {
            deleteRequest()
        }
    }

    private fun loadRequestDetails() {
        db.collection("requests").document(requestId).get()
            .addOnSuccessListener { doc ->
                findViewById<EditText>(R.id.etJobName).setText(doc.getString("jobName") ?: "")
                findViewById<EditText>(R.id.etJobType).setText(doc.getString("jobType") ?: "")
                findViewById<EditText>(R.id.etPrice).setText(doc.getString("price") ?: "")
                findViewById<EditText>(R.id.etHours).setText(doc.getString("hours") ?: "")
                findViewById<EditText>(R.id.etLocation).setText(doc.getString("location") ?: "")
                findViewById<EditText>(R.id.etSkill).setText(doc.getString("skill") ?: "")
                findViewById<EditText>(R.id.etExperience).setText(doc.getString("experience") ?: "")
                findViewById<EditText>(R.id.etStartTime).setText(doc.getString("startTime") ?: "")
                findViewById<EditText>(R.id.etUserName).setText(doc.getString("userName") ?: "")
            }
    }

    private fun loadApplicants() {
        db.collection("app")
            .whereEqualTo("requestId", requestId)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    tvNoApplicants.visibility = TextView.VISIBLE
                    return@addOnSuccessListener
                }

                val tempList = mutableListOf<Pair<String, String>>()
                val uids = result.documents.map { it.getString("idee") ?: "" }

                for (uid in uids) {
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { userDoc ->
                            val name = userDoc.getString("name") ?: "مجهول"
                            tempList.add(Pair(uid, name))

                            if (tempList.size == uids.size) {
                                applicants = tempList
                                val adapter = ApplicantAdapter(this, applicants)
                                listView.adapter = adapter
                                tvNoApplicants.visibility = TextView.GONE
                            }
                        }
                }
            }
    }

    private fun deleteRequest() {
        db.collection("requests").document(requestId).delete()
            .addOnSuccessListener {
                db.collection("app")
                    .whereEqualTo("requestId", requestId)
                    .get()
                    .addOnSuccessListener { apps ->
                        for (doc in apps.documents) {
                            db.collection("app").document(doc.id).delete()
                        }
                        Toast.makeText(this, "تم حذف الطلب", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        intent.putExtra("uid", uid)
                        startActivity(intent)
                        finish()
                    }
            }
    }

    override fun onResume() {
        super.onResume()
        listView.setOnItemClickListener { _, _, position, _ ->
            val (otherUid, _) = applicants[position]
            val intent = Intent(this, ViewProfile2Activity::class.java)
            intent.putExtra("currentUid", uid)
            intent.putExtra("otherUid", otherUid)
            startActivity(intent)
        }
    }
}
