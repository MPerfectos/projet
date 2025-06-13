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

class EmployerSignUpActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employer_sign_up)

        auth = FirebaseAuth.getInstance()

        val companyEt = findViewById<EditText>(R.id.companyNameEditText)
        val emailEt = findViewById<EditText>(R.id.emailEditText)
        val passEt = findViewById<EditText>(R.id.passwordEditText)
        val phoneEt = findViewById<EditText>(R.id.phoneEditText)
        val ageEt = findViewById<EditText>(R.id.AgeEditText)
        val nameEt = findViewById<EditText>(R.id.NameEditText)
        val locationEt = findViewById<EditText>(R.id.LocastionEditText)
        val descriptionEt = findViewById<EditText>(R.id.DescriptionEditText)
        val signUpBtn = findViewById<Button>(R.id.signUpButton)
        val goToSignInText = findViewById<TextView>(R.id.goToSignUpText)

        signUpBtn.setOnClickListener {
            val company = companyEt.text.toString().trim()
            val email = emailEt.text.toString().trim()
            val pass = passEt.text.toString().trim()
            val age = ageEt.text.toString().trim()
            val name = nameEt.text.toString().trim()
            val location = locationEt.text.toString().trim()
            val description = descriptionEt.text.toString().trim()
            val phone = phoneEt.text.toString().trim()

            if (company.isEmpty() || email.isEmpty() || pass.isEmpty() || phone.isEmpty() || age.isEmpty() || location.isEmpty() || name.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: return@addOnCompleteListener

                        val userMap = hashMapOf(
                            "uid" to uid,
                            "companyName" to company,
                            "email" to email,
                            "name" to name,
                            "phone" to phone,
                            "age" to age,
                            "location" to location,
                            "description" to description,
                            "role" to "employer",
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
                        Toast.makeText(this, " Sign up failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }

        }
        goToSignInText.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }

    }
}