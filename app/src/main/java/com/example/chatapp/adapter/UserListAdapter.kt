package com.example.chatapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.R
import com.example.chatapp.data.User

class UserListAdapter(private val onClick: (User) -> Unit) :
    RecyclerView.Adapter<UserListAdapter.ViewHolder>() {

    private val users = mutableListOf<User>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.username_text)
        val statusText: TextView = itemView.findViewById(R.id.status_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.username.text = user.username
        holder.statusText.text = if (user.isOnline) "Online" else "Offline"
        holder.itemView.setOnClickListener { onClick(user) }
    }

    override fun getItemCount() = users.size

    fun updateUsers(newUsers: List<User>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }

    fun filterUsers(query: String) {
        val filtered = users.filter { it.username.contains(query, ignoreCase = true) }
        updateUsers(filtered)
    }
}