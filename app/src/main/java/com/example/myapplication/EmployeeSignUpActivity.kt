package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class EmployeeSignUpActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employee_sign_up)

        auth = FirebaseAuth.getInstance()

        val nameEt = findViewById<EditText>(R.id.nameEditText)
        val emailEt = findViewById<EditText>(R.id.emailEditText)
        val passEt = findViewById<EditText>(R.id.passwordEditText)
        val phoneEt = findViewById<EditText>(R.id.phoneEditText)
        val signUpBtn = findViewById<Button>(R.id.signUpButton)
        val ageEt = findViewById<EditText>(R.id.AgeEditText)
        val skillEt = findViewById<EditText>(R.id.SkillEditText)
        val expEt = findViewById<EditText>(R.id.expEditText)
        val educationEt = findViewById<EditText>(R.id.EducationEditText)
        val goToSignInText = findViewById<TextView>(R.id.goToSignUpText)
        goToSignInText.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }
        signUpBtn.setOnClickListener {
            val name = nameEt.text.toString().trim()
            val email = emailEt.text.toString().trim()
            val pass = passEt.text.toString().trim()
            val phone = phoneEt.text.toString().trim()
            val age = ageEt.text.toString().trim()
            val skill = skillEt.text.toString().trim()
            val education = educationEt.text.toString().trim()
            val exp = expEt.text.toString().trim()
            if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || phone.isEmpty() || age.isEmpty() || education.isEmpty() || exp.isEmpty() || skill.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: return@addOnCompleteListener

                        val userMap = hashMapOf(
                            "uid" to uid,
                            "name" to name,
                            "email" to email,
                            "phone" to phone,
                            "age" to age,
                            "exp" to exp,
                            "education" to education,
                            "skill" to skill,
                            "role" to "employee",

                            "registeredAt" to Date()

                        )
                        db.collection("users").document(uid)
                            .set(userMap)
                            .addOnSuccessListener {

                                val intent = Intent(this, MainActivity::class.java)
                                intent.putExtra("USER_ID", uid)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Toast.makeText(this, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}