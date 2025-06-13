package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserDetailsActivity : AppCompatActivity() {

    private lateinit var tvUserName: TextView
    private lateinit var tvCompany: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvAge: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvDescription: TextView

    private lateinit var btnMessage: Button

    private lateinit var db: FirebaseFirestore
    private lateinit var currentUid: String
    private lateinit var otherUid: String // المستخدم المعروض

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_details)

        // ربط العناصر
        tvUserName = findViewById(R.id.tvUserName1)
        tvCompany = findViewById(R.id.tvCompany1)
        tvEmail = findViewById(R.id.tvEmail1)
        tvPhone = findViewById(R.id.tvPhone1)
        tvAge = findViewById(R.id.tvAge1)
        tvLocation = findViewById(R.id.tvLocation1)
        tvDescription = findViewById(R.id.tvDescription1)

        btnMessage = findViewById(R.id.btnMessage1)

        db = FirebaseFirestore.getInstance()
        currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        otherUid = intent.getStringExtra("userId").orEmpty()

        if (otherUid.isEmpty()) {
            Toast.makeText(this, "حدث خطأ في عرض المستخدم", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadUserDetails()


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

            val chatRef = db.collection("chats").document(chatId)
            chatRef.get().addOnSuccessListener { chatSnap ->
                if (!chatSnap.exists()) {
                    val chatData = hashMapOf(
                        "user1" to currentUid,
                        "user2" to otherUid,
                        "createdAt" to System.currentTimeMillis()
                    )
                    chatRef.set(chatData).addOnSuccessListener {
                        val notification = hashMapOf(
                            "fromId" to currentUid,
                            "toId" to otherUid,
                            "message" to "conversation started",
                            "timestamp" to System.currentTimeMillis()
                        )
                        db.collection("notifications").add(notification)
                            .addOnSuccessListener {
                                openChatRoom(chatId, currentUid)
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "فشل في إنشاء الإشعار", Toast.LENGTH_SHORT).show()
                            }
                    }.addOnFailureListener {
                        Toast.makeText(this, "فشل في إنشاء المحادثة", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    openChatRoom(chatId, currentUid)
                }
            }.addOnFailureListener {
                Toast.makeText(this, "خطأ في الوصول إلى المحادثة", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openChatRoom(chatId: String, userId: String) {
        val intent = Intent(this, ChatRoomActivity::class.java)
        intent.putExtra("chatId", chatId)
        intent.putExtra("userId", userId)
        startActivity(intent)
    }

    private fun loadUserDetails() {
        db.collection("users").document(otherUid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    tvUserName.text = doc.getString("name") ?: "غير محدد"
                    tvCompany.text ="Company Name: "+( doc.getString("company") ?: "غير محدد")
                    tvEmail.text = "Emaile : "+(doc.getString("email") ?: "غير محدد")
                    tvPhone.text = "Phone Number : "+(doc.getString("phone") ?: "غير محدد")
                    tvAge.text = "Age : "+(doc.getString("age") ?: "غير محدد")
                    tvLocation.text = "Location"+(doc.getString("location") ?: "غير محدد")
                    tvDescription.text = "Description : "+(doc.getString("description") ?: "غير محدد")
                } else {
                    Toast.makeText(this, "لم يتم العثور على بيانات المستخدم", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "فشل تحميل البيانات: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
