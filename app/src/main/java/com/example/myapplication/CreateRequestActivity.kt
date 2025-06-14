package com.example.myapplication

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import java.util.*

class CreateRequestActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var uid: String
    private var userName: String = ""

    // متغيرات خاصة بالموقع
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationGeoPoint: GeoPoint? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_request)

        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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
        val btnGetCurrentLocation = findViewById<Button>(R.id.btnGetCurrentLocation)

        // زر جديد لعرض الموقع على الخريطة
        val btnViewOnMap = findViewById<Button>(R.id.btnViewOnMap)

        // زر "موقعك حالياً"
        btnGetCurrentLocation.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
                return@setOnClickListener
            }

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        val address = addresses[0].getAddressLine(0)
                        editLocation.setText(address)
                        locationGeoPoint = GeoPoint(location.latitude, location.longitude)
                    } else {
                        Toast.makeText(this, "تعذر تحديد اسم الموقع", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "تعذر الحصول على الموقع الحالي", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // زر عرض على الخريطة
        btnViewOnMap.setOnClickListener {
            val location = editLocation.text.toString().trim()
            if (location.isEmpty()) {
                Toast.makeText(this, "يرجى إدخال الموقع أولاً", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // محاولة تحويل النص إلى إحداثيات إذا لم تكن محددة مسبقاً
            if (locationGeoPoint == null) {
                try {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addressList = geocoder.getFromLocationName(location, 1)
                    if (!addressList.isNullOrEmpty()) {
                        val address = addressList[0]
                        locationGeoPoint = GeoPoint(address.latitude, address.longitude)
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "تعذر تحديد إحداثيات الموقع", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // فتح الخريطة مع الموقع المحدد
            locationGeoPoint?.let { geoPoint ->
                val intent = Intent(this, MapsActivity::class.java)
                intent.putExtra("latitude", geoPoint.latitude)
                intent.putExtra("longitude", geoPoint.longitude)
                intent.putExtra("locationName", location)
                intent.putExtra("showSpecificLocation", true)
                intent.putExtra("jobName", editJobName.text.toString())
                intent.putExtra("price", editPrice.text.toString())
                intent.putExtra("userName", userName)
                startActivity(intent)
            }
        }

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

            // في حال لم يتم تحديد locationGeoPoint مسبقًا، يتم محاولة تحويل النص إلى إحداثيات
            if (locationGeoPoint == null) {
                try {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addressList = geocoder.getFromLocationName(location, 1)
                    if (!addressList.isNullOrEmpty()) {
                        val address = addressList[0]
                        locationGeoPoint = GeoPoint(address.latitude, address.longitude)
                    } else {
                        Toast.makeText(this, "الموقع غير صالح", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "حدث خطأ في تحديد الموقع", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val requestData = hashMapOf(
                "jobName" to jobName,
                "price" to price,
                "location" to location,
                "locationGeo" to locationGeoPoint,
                "hours" to editHours.text.toString().trim(),
                "skill" to editSkill.text.toString().trim(),
                "experience" to editExperience.text.toString().trim(),
                "jobType" to editJobType.text.toString().trim(),
                "startTime" to textStartTime.text.toString(),
                "uid" to uid,
                "userName" to userName,
                "createdAt" to Timestamp.now(),
                "isActive" to true // إضافة حقل لتتبع الطلبات النشطة
            )

            db.collection("requests").add(requestData)
                .addOnSuccessListener { documentReference ->
                    // إضافة معرف الوثيقة إلى البيانات
                    documentReference.update("requestId", documentReference.id)

                    Toast.makeText(this, "تم حفظ الطلب بنجاح", Toast.LENGTH_SHORT).show()

                    // فتح الخريطة لعرض الطلب المحفوظ حديثاً
                    locationGeoPoint?.let { geoPoint ->
                        val intent = Intent(this, MapsActivity::class.java)
                        intent.putExtra("latitude", geoPoint.latitude)
                        intent.putExtra("longitude", geoPoint.longitude)
                        intent.putExtra("locationName", location)
                        intent.putExtra("showSpecificLocation", true)
                        intent.putExtra("jobName", jobName)
                        intent.putExtra("price", price)
                        intent.putExtra("userName", userName)
                        intent.putExtra("requestId", documentReference.id)
                        intent.putExtra("creatorUid", uid)
                        intent.putExtra("currentUid", uid)
                        intent.putExtra("showJobCreated", true)
                        startActivity(intent)
                    }

                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "حدث خطأ أثناء الحفظ", Toast.LENGTH_SHORT).show()
                }
        }
    }
}