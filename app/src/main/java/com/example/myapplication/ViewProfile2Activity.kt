package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class ViewProfile2Activity : AppCompatActivity() {

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvAge: TextView
    private lateinit var tvEducation: TextView
    private lateinit var tvSkill: TextView
    private lateinit var tvExp: TextView
    private lateinit var btnMessage: Button

    private lateinit var db: FirebaseFirestore
    private lateinit var currentUid: String
    private lateinit var otherUid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_profile2)

        // ربط الواجهات
        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)
        tvPhone = findViewById(R.id.tvPhone)
        tvAge = findViewById(R.id.tvAge)
        tvEducation = findViewById(R.id.tvEducation)
        tvSkill = findViewById(R.id.tvSkill)
        tvExp = findViewById(R.id.tvExp)
        btnMessage = findViewById(R.id.btnMessage)

        // Firebase
        db = FirebaseFirestore.getInstance()

        // الحصول على المعرفات من الـ Intent
        currentUid = intent.getStringExtra("currentUid") ?: ""
        otherUid = intent.getStringExtra("otherUid") ?: ""

        if (otherUid.isEmpty()) {
            Toast.makeText(this, "خطأ في تحميل الملف: المعرف الآخر فارغ", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadUserInfo()

        btnMessage.setOnClickListener {
            if (currentUid.isEmpty()) {
                Toast.makeText(this, "لم يتم تسجيل الدخول", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentUid == otherUid) {
                Toast.makeText(this, "لا يمكنك مراسلة نفسك", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val chatId = if (currentUid < otherUid)
                "${currentUid}_$otherUid" else "${otherUid}_$currentUid"

            openChatRoom(chatId, currentUid)
        }
    }

    private fun loadUserInfo() {
        db.collection("users").document(otherUid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    tvName.text = doc.getString("name") ?: "غير معروف"
                    tvEmail.text = doc.getString("email") ?: "غير معروف"
                    tvPhone.text = doc.getString("phone") ?: "غير معروف"
                    tvAge.text = doc.getString("age") ?: "غير معروف"
                    tvEducation.text = doc.getString("education") ?: "غير معروف"
                    tvSkill.text = doc.getString("skill") ?: "غير معروف"
                    tvExp.text = doc.getString("exp") ?: "غير معروف"
                } else {
                    Toast.makeText(this, "المستخدم غير موجود في قاعدة البيانات", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "فشل في تحميل بيانات المستخدم", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openChatRoom(chatId: String, userId: String) {
        val intent = Intent(this, ChatRoomActivity::class.java)
        intent.putExtra("chatId", chatId)
        intent.putExtra("userId", userId)
        startActivity(intent)
    }
}
