package com.example.model

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreService(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()

    fun getChatsFlow(email: String): Flow<List<ChatSession>> = callbackFlow {
        if (email.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val chatsRef = firestore.collection("chats").whereEqualTo("userId", email)
        val registration = chatsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val sessions = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.getString("id") ?: return@mapNotNull null
                        val title = doc.getString("title") ?: "New Chat"
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        ChatSession(id, title, emptyList(), timestamp)
                    } catch(e: Exception) {
                        null
                    }
                }
                trySend(sessions.sortedByDescending { it.timestamp })
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { registration.remove() }
    }

    fun getMessagesFlow(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        if (chatId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val msgsRef = firestore.collection("messages").whereEqualTo("chatId", chatId)
        val registration = msgsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val messages = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.getString("id") ?: return@mapNotNull null
                        val text = doc.getString("text") ?: ""
                        val isUser = doc.getBoolean("isUser") ?: false
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        ChatMessage(id = id, text = text, isUser = isUser, timestamp = timestamp)
                    } catch(e: Exception) {
                        null
                    }
                }
                trySend(messages.sortedBy { it.timestamp })
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveSession(email: String, session: ChatSession) {
        if (email.isBlank()) return
        try {
            firestore.collection("users").document(email)
                .set(mapOf(
                    "userId" to email,
                    "email" to email,
                    "lastActive" to System.currentTimeMillis()
                ), SetOptions.merge())

            val chatData = mapOf(
                "id" to session.id,
                "userId" to email,
                "title" to session.title,
                "timestamp" to session.timestamp
            )
            firestore.collection("chats").document(session.id).set(chatData, SetOptions.merge())
            
            for (msg in session.messages) {
                saveMessage(email, session.id, msg)
            }
        } catch (e: Exception) {
            // Ignore for now
        }
    }

    suspend fun saveMessage(email: String, chatId: String, msg: ChatMessage) {
        if (email.isBlank()) return
        try {
            val msgData = mapOf(
                "id" to msg.id,
                "chatId" to chatId,
                "userId" to email,
                "role" to if(msg.isUser) "user" else "assistant",
                "text" to msg.text,
                "isUser" to msg.isUser,
                "timestamp" to msg.timestamp
            )
            firestore.collection("messages").document(msg.id).set(msgData, SetOptions.merge())
        } catch (e: Exception) {
            // Ignore for now
        }
    }

    suspend fun deleteSession(chatId: String) {
        try {
            firestore.collection("chats").document(chatId).delete().await()
            val msgs = firestore.collection("messages").whereEqualTo("chatId", chatId).get().await()
            for (doc in msgs) {
                doc.reference.delete()
            }
        } catch (e: Exception) {}
    }

    suspend fun clearAll(email: String) {
        try {
            val chats = firestore.collection("chats").whereEqualTo("userId", email).get().await()
            val msgs = firestore.collection("messages").whereEqualTo("userId", email).get().await()
            for(doc in chats) doc.reference.delete()
            for(doc in msgs) doc.reference.delete()
        } catch (e: Exception) {}
    }

    suspend fun saveSettings(email: String, isDarkTheme: Boolean, themeColor: String = "GREEN") {
        if (email.isBlank()) return
        try {
            firestore.collection("settings").document(email)
                .set(mapOf("isDarkTheme" to isDarkTheme, "themeColor" to themeColor), SetOptions.merge())
        } catch (e: Exception) {}
    }

    fun getSettingsFlow(email: String): Flow<Pair<Boolean?, String>> = callbackFlow {
        if (email.isBlank()) {
            trySend(Pair(null, "GREEN"))
            close()
            return@callbackFlow
        }
        val settingsRef = firestore.collection("settings").document(email)
        val registration = settingsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Pair(null, "GREEN"))
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                trySend(Pair(snapshot.getBoolean("isDarkTheme"), snapshot.getString("themeColor") ?: "GREEN"))
            } else {
                trySend(Pair(null, "GREEN"))
            }
        }
        awaitClose { registration.remove() }
    }
}
