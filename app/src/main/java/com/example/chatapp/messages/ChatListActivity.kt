package com.example.chatapp.messages

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.R
import com.example.chatapp.adapter.ChatListAdapter
import com.example.chatapp.data.ChatMetadata
import com.example.chatapp.data.User
import com.example.chatapp.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatListActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatListAdapter
    private lateinit var emptyText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        title = "Chat List"

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        recyclerView = findViewById(R.id.chat_list_recycler)
        emptyText = findViewById(R.id.empty_text)
        progressBar = findViewById(R.id.progress_bar)

        adapter = ChatListAdapter { chatMetadata ->
            // Здесь нужно получить User из ChatMetadata и передать в ChatActivity
            // Получаем ID собеседника из списка участников
            val otherUserUid = chatMetadata.participants.find { it != auth.currentUser?.uid }

            otherUserUid?.let { uid ->
                // Загружаем информацию о пользователе по ID
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { snapshot ->
                        val user = snapshot.toObject(User::class.java)
                        if (user != null) {
                            val intent = Intent(this, ChatActivity::class.java)
                            intent.putExtra("USER", user as java.io.Serializable)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Could not load user information", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadChats()
        updateUserStatus(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_chat_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logout()
                true
            }
            R.id.action_refresh -> {
                loadChats()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadChats() {
        progressBar.visibility = View.VISIBLE
        emptyText.visibility = View.GONE

        val currentUser = auth.currentUser ?: return

        db.collection("chats")
            .whereArrayContains("participants", currentUser.uid)
            .addSnapshotListener { snapshot, e ->
                progressBar.visibility = View.GONE

                if (e != null) {
                    Log.e("ChatApp", "Error loading chats: ${e.message}")
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val chats = snapshot?.documents
                    ?.mapNotNull { it.toObject(ChatMetadata::class.java) }
                    ?: emptyList()

                Log.d("ChatApp", "Loaded ${chats.size} chats")

                adapter.updateChats(chats)

                if (chats.isEmpty()) {
                    emptyText.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyText.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
    }

    private fun logout() {
        updateUserStatus(false)
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun updateUserStatus(isOnline: Boolean) {
        auth.currentUser?.let { user ->
            db.collection("users").document(user.uid)
                .update(
                    mapOf(
                        "isOnline" to isOnline,
                        "lastActive" to System.currentTimeMillis()
                    )
                )
        }
    }

    override fun onResume() {
        super.onResume()
        updateUserStatus(true)
    }

    override fun onPause() {
        super.onPause()
        updateUserStatus(false)
    }
}