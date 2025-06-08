package com.example.myapplication.model

data class JobRequest(
    val personName: String = "",
    val jobTitle: String = "",
    val jobType: String = "",
    val salary: String = "",
    val createdByUid: String = "",
    val requestId: String = ""

)
