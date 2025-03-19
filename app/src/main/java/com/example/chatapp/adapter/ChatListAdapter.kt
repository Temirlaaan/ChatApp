package com.example.chatapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.R
import com.example.chatapp.data.ChatMetadata
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatListAdapter(private val onClick: (ChatMetadata) -> Unit) :
    RecyclerView.Adapter<ChatListAdapter.ViewHolder>() {

    private val chats = mutableListOf<ChatMetadata>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.username_text)
        val lastMessage: TextView = itemView.findViewById(R.id.last_message_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = chats[position]
        val otherUid = chat.participants.find { it != FirebaseAuth.getInstance().currentUser?.uid }
        holder.lastMessage.text = chat.lastMessage
        holder.itemView.setOnClickListener { onClick(chat) }

        // Асинхронная загрузка имени пользователя
        otherUid?.let { uid ->
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener { snapshot ->
                    val username = snapshot.getString("username") ?: "Unknown"
                    holder.username.text = username
                }
                .addOnFailureListener {
                    holder.username.text = "Unknown"
                }
        } ?: run {
            holder.username.text = "Unknown"
        }
    }

    override fun getItemCount() = chats.size

    fun updateChats(newChats: List<ChatMetadata>) {
        chats.clear()
        chats.addAll(newChats.sortedByDescending { it.lastMessageTime })
        notifyDataSetChanged()
    }
}