package com.example.myapplication

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyProfileActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    // الواجهة الرئيسية
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    // الأيقونة لفتح الدرج
    private lateinit var settingsIcon: ImageView

    // حقول عرض البيانات
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvAge: TextView

    // مجموعات إضافية بحسب النوع
    private lateinit var groupEmployee: LinearLayout
    private lateinit var tvSkill: TextView
    private lateinit var tvEducation: TextView
    private lateinit var tvExp: TextView

    private lateinit var groupEmployer: LinearLayout
    private lateinit var tvCompany: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvLocationEmployer: TextView


    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var currentUid: String
    private lateinit var role: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_profile)

        // ربط العناصر
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        settingsIcon = findViewById(R.id.settingsIcon)

        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)
        tvPhone = findViewById(R.id.tvPhone)
        tvAge = findViewById(R.id.tvAge)

        groupEmployee = findViewById(R.id.groupEmployee)
        tvSkill = findViewById(R.id.tvSkill)
        tvEducation = findViewById(R.id.tvEducation)
        tvExp = findViewById(R.id.tvExp)

        groupEmployer = findViewById(R.id.groupEmployer)
        tvCompany = findViewById(R.id.tvCompany)
        tvDescription = findViewById(R.id.tvDescription)
        tvLocationEmployer = findViewById(R.id.tvLocation)

        // اضبط الـ NavigationItemSelectedListener
        navView.setNavigationItemSelectedListener(this)

        // تيجي قيمة الUID و role من Intent
        currentUid = intent.getStringExtra("uid") ?: ""
        role = intent.getStringExtra("role") ?: ""

        // إذا لم تمرر بيانات صحيحة
        if (currentUid.isEmpty() || role.isEmpty()) {
            Toast.makeText(this, "خطأ في جلب بيانات المستخدم", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // عند الضغط على أيقونة الإعدادات افتح الـ Drawer
        settingsIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // جلب بيانات المستخدم من Firestore
        loadUserData()
    }

    private fun loadUserData() {
        // المستند في مجموعة “users” يحمل UID
        db.collection("users").document(currentUid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    // بيانات عامة
                    tvName.text =" Name : " + (doc.getString("name") ?: "")
                    tvEmail.text ="Email : "+( doc.getString("email") ?: "")
                    tvPhone.text ="phone Number : "+( doc.getString("phone") ?: "")
                    tvAge.text = "Age : "+(doc.getString("age") ?: "")

                    // أظهر المخفي بناءً على الدور
                    if (role == "employee") {
                        groupEmployee.visibility = View.VISIBLE
                        groupEmployer.visibility = View.GONE

                        tvSkill.text ="Skills : " +(doc.getString("skill") ?: "")
                        tvEducation.text ="Education : "+ (doc.getString("education") ?: "")
                        tvExp.text = "experience : "+(doc.getString("exp") ?: "")
                    } else {
                        // employer
                        groupEmployee.visibility = View.GONE
                        groupEmployer.visibility = View.VISIBLE

                        tvCompany.text = "Company Name : "+(doc.getString("companyName") ?: "")
                        tvDescription.text = "Description : "+(doc.getString("description") ?: "")
                        tvLocationEmployer.text = "Location : "+(doc.getString("location") ?: "")
                    }
                } else {
                    Toast.makeText(this, "المستخدم غير موجود", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "فشل في تحميل البيانات: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // عند اختيار عنصر من قائمة الـ Drawer
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.naveditprofile -> {
                // فتح Dialog التحرير حسب الدور
                if (role == "employee") {
                    showEditEmployeeDialog()
                } else {
                    showEditEmployerDialog()
                }
            }

            R.id.nav_language -> {
                showLanguageDialog()
            }

            R.id.navdarkmode -> {
                // لا يُنفّذ أي تغيير فعليّ، Toggle فقط
                val switchView = item.actionView!!.findViewById<Switch>(R.id.switchDarkMode)
                // يمكنك لاحقاً استخدام switchView.isChecked لمعرفة الحالة
                // حالياً لا نقوم بأي تغيير في ثيم التطبيق
                Toast.makeText(this, "الوضع الداكن: ${if (switchView.isChecked) "تشغيل" else "إيقاف"}", Toast.LENGTH_SHORT).show()
            }

            R.id.nav_sign_out -> {
                auth.signOut()
                startActivity(Intent(this, SignInActivity::class.java))
                finish()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    // ====== 6.A: Dialog تحرير الموظف ======
    private fun showEditEmployeeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_employee, null)
        val editName = dialogView.findViewById<EditText>(R.id.editName)
        val editPhone = dialogView.findViewById<EditText>(R.id.editPhone)
        val editAge = dialogView.findViewById<EditText>(R.id.editAge)
        val editEducation = dialogView.findViewById<EditText>(R.id.editEducation)
        val editSkill = dialogView.findViewById<EditText>(R.id.editSkill)
        val editExp = dialogView.findViewById<EditText>(R.id.editExp)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveEmployee)

        // جلب بيانات حالية لتعبئة الحقول
        db.collection("users").document(currentUid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    editName.setText(doc.getString("name"))
                    editPhone.setText(doc.getString("phone"))
                    editAge.setText(doc.getString("age"))
                    editEducation.setText(doc.getString("education"))
                    editSkill.setText(doc.getString("skill"))
                    editExp.setText(doc.getString("exp"))
                }
            }

        val alertDlg = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnSave.setOnClickListener {
            val newName = editName.text.toString().trim()
            val newPhone = editPhone.text.toString().trim()
            val newAge = editAge.text.toString().trim()
            val newEdu = editEducation.text.toString().trim()
            val newSkill = editSkill.text.toString().trim()
            val newExp = editExp.text.toString().trim()

            if (newName.isEmpty() || newPhone.isEmpty() || newAge.isEmpty() ||
                newEdu.isEmpty() || newSkill.isEmpty() || newExp.isEmpty()
            ) {
                Toast.makeText(this, "يرجى ملء جميع الحقول", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // تحديث Firestore
            val updates = hashMapOf<String, Any>(
                "name" to newName,
                "phone" to newPhone,
                "age" to newAge,
                "education" to newEdu,
                "skill" to newSkill,
                "exp" to newExp
            )
            db.collection("users").document(currentUid)
                .update(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "تم تحديث الملف الشخصي", Toast.LENGTH_SHORT).show()
                    alertDlg.dismiss()
                    // إعادة تحميل البيانات في الواجهة
                    loadUserData()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "فشل التحديث: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        alertDlg.show()
    }

    // ====== 6.B: Dialog تحرير صاحب العمل ======
    private fun showEditEmployerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_employer, null)
        val editName = dialogView.findViewById<EditText>(R.id.editName)
        val editPhone = dialogView.findViewById<EditText>(R.id.editPhone)
        val editAge = dialogView.findViewById<EditText>(R.id.editAge)
        val editCompany = dialogView.findViewById<EditText>(R.id.editCompany)
        val editDescription = dialogView.findViewById<EditText>(R.id.editDescription)
        val editLocation = dialogView.findViewById<EditText>(R.id.editLocation)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveEmployer)

        // جلب بيانات حالية
        db.collection("users").document(currentUid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    editName.setText(doc.getString("name"))
                    editPhone.setText(doc.getString("phone"))
                    editAge.setText(doc.getString("age"))
                    editCompany.setText(doc.getString("companyName"))
                    editDescription.setText(doc.getString("description"))
                    editLocation.setText(doc.getString("location"))
                }
            }

        val alertDlg = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnSave.setOnClickListener {
            val newName = editName.text.toString().trim()
            val newPhone = editPhone.text.toString().trim()
            val newAge = editAge.text.toString().trim()
            val newCompany = editCompany.text.toString().trim()
            val newDesc = editDescription.text.toString().trim()
            val newLoc = editLocation.text.toString().trim()

            if (newName.isEmpty() || newPhone.isEmpty() || newAge.isEmpty() ||
                newCompany.isEmpty() || newDesc.isEmpty() || newLoc.isEmpty()
            ) {
                Toast.makeText(this, "يرجى ملء جميع الحقول", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // تحديث Firestore
            val updates = hashMapOf<String, Any>(
                "name" to newName,
                "phone" to newPhone,
                "age" to newAge,
                "companyName" to newCompany,
                "description" to newDesc,
                "location" to newLoc
            )
            db.collection("users").document(currentUid)
                .update(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "تم تحديث الملف الشخصي", Toast.LENGTH_SHORT).show()
                    alertDlg.dismiss()
                    loadUserData()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "فشل التحديث: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        alertDlg.show()
    }

    // ====== 6.C: Dialog اختيار اللغة ======
    private fun showLanguageDialog() {
        // خيارات اللغة
        val languages = arrayOf("العربية", "English", "Français")
        var selectedIndex = 0

        AlertDialog.Builder(this)
            .setTitle("Choose a language ")
            .setSingleChoiceItems(languages, selectedIndex) { dialog, which ->
                selectedIndex = which
            }
            .setPositiveButton("Save") { dialog, _ ->
                // هنا يمكنك حفظ الخيار في SharedPreferences مثلاً
                Toast.makeText(this, "تم اختيار: ${languages[selectedIndex]}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // عند الضغط على زر العودة (Back) وأرى الدرج مفتوحًا، أغلقه أولًا
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
