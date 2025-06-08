package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ChooseRoleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_role)
        val goToSignInText = findViewById<TextView>(R.id.goToSignUpText)
        goToSignInText.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }
        findViewById<Button>(R.id.employeeButton).setOnClickListener {
            startActivity(Intent(this, EmployeeSignUpActivity::class.java))
        }
        findViewById<Button>(R.id.employerButton).setOnClickListener {
            startActivity(Intent(this, EmployerSignUpActivity::class.java))
        }
    }

}