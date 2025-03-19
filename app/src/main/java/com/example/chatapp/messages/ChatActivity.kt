package com.example.chatapp.messages

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chatapp.databinding.ActivityChatBinding
import com.example.chatapp.adapter.MessagesAdapter
import com.example.chatapp.data.Message
import com.example.chatapp.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: MessagesAdapter
    private lateinit var recipient: User
    private lateinit var chatId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recipient = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("USER", User::class.java) ?: throw IllegalStateException("User not provided")
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("USER") as User
        }

        chatId = generateChatId(FirebaseAuth.getInstance().currentUser!!.uid, recipient.uid)
        title = recipient.username
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = MessagesAdapter(FirebaseAuth.getInstance().currentUser!!.uid)
        binding.messagesRecycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.messagesRecycler.adapter = adapter

        binding.sendButton.setOnClickListener {
            val text = binding.messageInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                binding.messageInput.text?.clear()
            }
        }

        createChatIfNeeded()
        listenForMessages()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun createChatIfNeeded() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val chatData = hashMapOf(
            "participants" to listOf(currentUser.uid, recipient.uid),
            "lastActive" to FieldValue.serverTimestamp()
        )
        FirebaseFirestore.getInstance().collection("chats").document(chatId)
            .set(chatData, com.google.firebase.firestore.SetOptions.merge())
    }

    private fun sendMessage(text: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        binding.sendButton.isEnabled = false

        val timestamp = System.currentTimeMillis()
        val message = hashMapOf(
            "text" to text,
            "senderId" to currentUser.uid,
            "senderName" to currentUser.email?.substringBefore('@'),
            "timestamp" to timestamp,
            "isDelivered" to false,
            "isRead" to false
        )

        FirebaseFirestore.getInstance().collection("chats").document(chatId).collection("messages")
            .add(message)
            .addOnSuccessListener { doc ->
                // Исправлено: используем правильный синтаксис для update
                doc.update(mapOf("id" to doc.id))
                binding.sendButton.isEnabled = true
                FirebaseFirestore.getInstance().collection("chats").document(chatId)
                    .update(
                        mapOf(
                            "lastMessage" to text,
                            "lastMessageTime" to timestamp,
                            "lastMessageSenderId" to currentUser.uid
                        )
                    )
            }
            .addOnFailureListener { e ->
                binding.sendButton.isEnabled = true
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun listenForMessages() {
        binding.messagesProgress.visibility = android.view.View.VISIBLE
        FirebaseFirestore.getInstance().collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                binding.messagesProgress.visibility = android.view.View.GONE
                if (e != null) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.apply {
                        if (senderId != FirebaseAuth.getInstance().currentUser?.uid && !isRead) {
                            // Исправлено: используем правильный синтаксис для update
                            doc.reference.update(mapOf(
                                "isDelivered" to true,
                                "isRead" to true
                            ))
                        }
                    }
                } ?: emptyList()

                adapter.updateMessages(messages)
                if (messages.isNotEmpty()) binding.messagesRecycler.scrollToPosition(messages.size - 1)
            }
    }

    private fun generateChatId(user1: String, user2: String): String {
        return if (user1 < user2) "$user1-$user2" else "$user2-$user1"
    }
}