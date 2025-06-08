package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class ChooseRoleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_role)
        val goToSignInText = findViewById<TextView>(R.id.signInText)
        goToSignInText.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }
        findViewById<CardView>(R.id.employeeCard).setOnClickListener {
            startActivity(Intent(this, EmployeeSignUpActivity::class.java))
        }
        findViewById<CardView>(R.id.employerCard).setOnClickListener {
            startActivity(Intent(this, EmployerSignUpActivity::class.java))
        }
    }
}