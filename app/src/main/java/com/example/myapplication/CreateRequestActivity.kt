package com.example.myapplication

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class CreateRequestActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var uid: String
    private var userName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_request)

        db = FirebaseFirestore.getInstance()
        uid = intent.getStringExtra("uid") ?: ""

        if (uid.isEmpty()) {
            Toast.makeText(this, "لم يتم تمرير uid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val editJobName = findViewById<EditText>(R.id.editJobName)
        val editPrice = findViewById<EditText>(R.id.editPrice)
        val editLocation = findViewById<EditText>(R.id.editLocation)
        val editHours = findViewById<EditText>(R.id.editHours)
        val editSkill = findViewById<EditText>(R.id.editSkill)
        val editExperience = findViewById<EditText>(R.id.editExperience)
        val editJobType = findViewById<EditText>(R.id.editJobType)
        val textStartTime = findViewById<EditText>(R.id.editTextStartTime)
        val btnSave = findViewById<Button>(R.id.btnSaveRequest)

        // جلب اسم المستخدم
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            userName = doc.getString("name") ?: ""
        }

        // فتح Dialog عند الضغط على وقت البدء
        textStartTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    val time = String.format("%02d:%02d", hour, minute)
                    textStartTime.setText(time)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }


        // عند الضغط على زر الحفظ
        btnSave.setOnClickListener {
            val jobName = editJobName.text.toString().trim()
            val price = editPrice.text.toString().trim()
            val location = editLocation.text.toString().trim()

            if (jobName.isEmpty() || price.isEmpty() || location.isEmpty()) {
                Toast.makeText(this, "يرجى إدخال اسم العمل والسعر والموقع", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val requestData = hashMapOf(
                "jobName" to jobName,
                "price" to price,
                "location" to location,
                "hours" to editHours.text.toString().trim(),
                "skill" to editSkill.text.toString().trim(),
                "experience" to editExperience.text.toString().trim(),
                "jobType" to editJobType.text.toString().trim(),
                "startTime" to textStartTime.text.toString(),
                "uid" to uid,
                "userName" to userName,
                "createdAt" to Timestamp.now()
            )

            db.collection("requests").add(requestData)
                .addOnSuccessListener {
                    Toast.makeText(this, "تم حفظ الطلب بنجاح", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "حدث خطأ أثناء الحفظ", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
