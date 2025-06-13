package com.example.myapplication.model

import org.osmdroid.util.GeoPoint

data class LocationData(
    val name: String,
    val rating: Double,
    val reviews: Int,
    val distance: Double,
    val price: Int,
    val currency: String = "DZD",
    val geoPoint: GeoPoint,
    val imageRes: Int = 0
)
