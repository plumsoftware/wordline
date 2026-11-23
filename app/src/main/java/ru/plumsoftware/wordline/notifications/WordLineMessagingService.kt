package ru.plumsoftware.wordline.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class WordLineMessagingService: FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "Соедини слова"
        val body = message.notification?.body ?: message.data["body"] ?: return
        GameNotifications.show(this, title, body, tasks = message.data["destination"] == "tasks",
            id = message.messageId?.hashCode() ?: 1002)
    }
}
