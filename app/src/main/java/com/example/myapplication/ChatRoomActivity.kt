package com.example.myapplication

import android.os.Bundle
import android.view.KeyEvent
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.adapters.MessageAdapter
import com.example.myapplication.model.Message
import com.google.firebase.firestore.*

class ChatRoomActivity : AppCompatActivity() {

    private var lvMessages: ListView? = null
    private var etMessage: EditText? = null
    private var adapter: MessageAdapter? = null
    private val messagesList = mutableListOf<Message>()

    private lateinit var firestore: FirebaseFirestore
    private var messagesCollection: CollectionReference? = null

    private lateinit var currentUserId: String
    private lateinit var chatId: String

    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)


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
        if (lvMessages == null || etMessage == null) {
            Toast.makeText(this, "خطأ في واجهة المحادثة: تحقق من وجود العناصر", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 3. تهيئة Adapter وربطه بالـ ListView
        adapter = MessageAdapter(this, messagesList, currentUserId)
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

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }
}
