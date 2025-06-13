package com.example.myapplication

/**
 * كلاس يمثل نموذج الإشعار المخزن في Firestore.
 * الحقول: fromId (UID للمرسل)، toId (UID للمستلم)، message (نص الرسالة)، timestamp (وقت الإنشاء)
 */
data class NotificationData(
    val otherUserId: String,
    val message: String,
    val timestamp: Long
)