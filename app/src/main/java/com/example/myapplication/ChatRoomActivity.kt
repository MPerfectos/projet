package com.example.myapplication

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.KeyEvent
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.MessageAdapter
import com.example.myapplication.model.Message
import com.google.firebase.firestore.*

class ChatRoomActivity : AppCompatActivity() {

    private var lvMessages: ListView? = null
    private var etMessage: EditText? = null
    private var btnSend: ImageView? = null
    private var btnBack: ImageView? = null
    private var btnEmoji: ImageView? = null
    private var btnAttachment: ImageView? = null
    private var adapter: MessageAdapter? = null
    private val messagesList = mutableListOf<Message>()

    private lateinit var firestore: FirebaseFirestore
    private var messagesCollection: CollectionReference? = null

    private lateinit var currentUserId: String
    private lateinit var chatId: String

    private var listenerRegistration: ListenerRegistration? = null

    // Activity result launchers for file picking
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleSelectedFile(it, "image") }
    }

    private val documentPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleSelectedFile(it, "document") }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            // Handle camera result if needed
            Toast.makeText(this, "تم التقاط الصورة بنجاح", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)

        // 1. قراءة chatId و userId من الـ Intent
        chatId = intent.getStringExtra("chatId").orEmpty()
        currentUserId = intent.getStringExtra("userId").orEmpty()

        // 1.a: التحقق من أن القيم غير فارغة
        if (chatId.isEmpty() || currentUserId.isEmpty()) {
            Toast.makeText(this, "بيانات المحادثة غير مكتملة", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2. ربط عناصر الواجهة والتأكد من وجودها
        lvMessages = findViewById(R.id.lvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnBack = findViewById(R.id.btnBack)
        btnEmoji = findViewById(R.id.btnEmoji)
        btnAttachment = findViewById(R.id.btnAttachment)

        if (lvMessages == null || etMessage == null || btnSend == null || btnBack == null) {
            Toast.makeText(this, "خطأ في واجهة المحادثة: تحقق من وجود العناصر", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 3. تهيئة Adapter وربطه بالـ ListView مع تمرير callback للحذف
        adapter = MessageAdapter(this, messagesList, currentUserId) { message ->
            showDeleteMessageDialog(message)
        }
        lvMessages?.adapter = adapter

        // 4. تهيئة Firestore ومجموعة الرسائل الخاصة بالمحادثة
        firestore = FirebaseFirestore.getInstance()
        messagesCollection = firestore
            .collection("chats")
            .document(chatId)
            .collection("messages")

        if (messagesCollection == null) {
            Toast.makeText(this, "تعذر الوصول إلى مجموعة الرسائل", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 5. الاستماع للتحديثات وعرض الرسائل مباشرة
        listenForMessages()

        // 6. إرسال الرسالة عند الضغط على Enter في لوحة المفاتيح
        etMessage?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                sendMessage()
                true
            } else {
                false
            }
        }

        // 7. إرسال الرسالة عند الضغط على زر الإرسال
        btnSend?.setOnClickListener {
            sendMessage()
        }

        // 8. إعداد زر الرجوع
        btnBack?.setOnClickListener {
            finish() // العودة إلى الواجهة السابقة
        }

        // 9. إعداد زر الإيموجي
        btnEmoji?.setOnClickListener {
            showEmojiMenu()
        }

        // 10. إعداد زر المرفقات
        btnAttachment?.setOnClickListener {
            showAttachmentMenu()
        }

        // Remove the ListView long click listener since it's now handled in the adapter
    }

    private fun listenForMessages() {
        try {
            listenerRegistration = messagesCollection
                ?.orderBy("timestamp", Query.Direction.ASCENDING)
                ?.addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Toast.makeText(this, "خطأ في جلب الرسائل: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    if (snapshots == null) return@addSnapshotListener

                    // تنظيف القائمة وإعادة تعبئتها بالرسائل المجلوبة
                    messagesList.clear()
                    for (doc in snapshots.documents) {
                        val msg = doc.toObject(Message::class.java)
                        if (msg != null) {
                            messagesList.add(msg)
                        }
                    }
                    adapter?.notifyDataSetChanged()
                    // تمرير ListView لأسفل ليظهر آخر رسالة
                    lvMessages?.post {
                        lvMessages?.setSelection(messagesList.size - 1)
                    }
                }
        } catch (ex: Exception) {
            Toast.makeText(this, "استثناء في الاستماع للرسائل: ${ex.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendMessage() {
        val content = etMessage?.text.toString().trim()
        if (content.isEmpty()) return

        val newMsg = Message(
            senderId = currentUserId,
            content = content,
            timestamp = System.currentTimeMillis()
        )

        try {
            messagesCollection
                ?.add(newMsg)
                ?.addOnSuccessListener {
                    etMessage?.text?.clear()
                }
                ?.addOnFailureListener { ex ->
                    Toast.makeText(this, "فشل في إرسال الرسالة: ${ex.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (ex: Exception) {
            Toast.makeText(this, "استثناء أثناء الإرسال: ${ex.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteMessageDialog(message: Message) {
        // التحقق من أن الرسالة ملك للمستخدم الحالي
        if (message.senderId != currentUserId) {
            Toast.makeText(this, "لا يمكنك حذف رسائل الآخرين", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("حذف الرسالة")
            .setMessage("هل تريد حذف هذه الرسالة؟")
            .setPositiveButton("حذف") { _, _ ->
                deleteMessage(message)
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun deleteMessage(message: Message) {
        try {
            // البحث عن الرسالة في Firestore باستخدام timestamp و senderId
            messagesCollection
                ?.whereEqualTo("timestamp", message.timestamp)
                ?.whereEqualTo("senderId", message.senderId)
                ?.whereEqualTo("content", message.content)
                ?.get()
                ?.addOnSuccessListener { documents ->
                    for (document in documents) {
                        document.reference.delete()
                            .addOnSuccessListener {
                                Toast.makeText(this, "تم حذف الرسالة", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { ex ->
                                Toast.makeText(this, "فشل في حذف الرسالة: ${ex.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                ?.addOnFailureListener { ex ->
                    Toast.makeText(this, "خطأ في البحث عن الرسالة: ${ex.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (ex: Exception) {
            Toast.makeText(this, "استثناء أثناء الحذف: ${ex.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEmojiMenu() {
        val emojis = arrayOf("😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
            "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚",
            "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🤩",
            "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣",
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔",
            "👍", "👎", "👌", "🤌", "🤏", "✌️", "🤞", "🤟", "🤘", "🤙")

        AlertDialog.Builder(this)
            .setTitle("اختر إيموجي")
            .setItems(emojis) { _, which ->
                val currentText = etMessage?.text.toString()
                val cursorPosition = etMessage?.selectionStart ?: currentText.length
                val newText = StringBuilder(currentText).insert(cursorPosition, emojis[which]).toString()
                etMessage?.setText(newText)
                etMessage?.setSelection(cursorPosition + emojis[which].length)
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showAttachmentMenu() {
        val options = arrayOf("صورة من المعرض", "التقاط صورة", "ملف")

        AlertDialog.Builder(this)
            .setTitle("إرفاق ملف")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openImagePicker()
                    1 -> openCamera()
                    2 -> openDocumentPicker()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun openImagePicker() {
        try {
            imagePickerLauncher.launch("image/*")
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ في فتح معرض الصور: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCamera() {
        try {
            // For now, just show a toast. You can implement camera functionality later
            Toast.makeText(this, "ميزة الكاميرا قيد التطوير", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ في فتح الكاميرا: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openDocumentPicker() {
        try {
            documentPickerLauncher.launch("*/*")
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ في فتح منتقي الملفات: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSelectedFile(uri: Uri, type: String) {
        try {
            // For now, just show the file name. You can implement file upload logic later
            val fileName = getFileName(uri)
            val message = when (type) {
                "image" -> "📷 صورة: $fileName"
                "document" -> "📄 ملف: $fileName"
                else -> "📎 مرفق: $fileName"
            }

            // Send the file reference as a message
            sendFileMessage(message)

        } catch (e: Exception) {
            Toast.makeText(this, "خطأ في معالجة الملف: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "ملف غير معروف"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex) ?: "ملف غير معروف"
                    }
                }
            }
        } catch (e: Exception) {
            // Keep default name if query fails
        }
        return fileName
    }

    private fun sendFileMessage(content: String) {
        val newMsg = Message(
            senderId = currentUserId,
            content = content,
            timestamp = System.currentTimeMillis()
        )

        try {
            messagesCollection
                ?.add(newMsg)
                ?.addOnSuccessListener {
                    Toast.makeText(this, "تم إرسال المرفق", Toast.LENGTH_SHORT).show()
                }
                ?.addOnFailureListener { ex ->
                    Toast.makeText(this, "فشل في إرسال المرفق: ${ex.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (ex: Exception) {
            Toast.makeText(this, "استثناء أثناء إرسال المرفق: ${ex.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }
}