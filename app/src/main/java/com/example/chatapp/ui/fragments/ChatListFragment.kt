package com.example.chatapp.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatapp.databinding.FragmentChatListBinding
import com.example.chatapp.adapter.ChatListAdapter
import com.example.chatapp.data.ChatMetadata
import com.example.chatapp.messages.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ChatListFragment : Fragment() {
    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ChatListAdapter
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ChatListAdapter { chat ->
            val recipientUid = chat.participants.find { it != FirebaseAuth.getInstance().currentUser?.uid }
            recipientUid?.let { uid ->
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .get()
                    .addOnSuccessListener { document ->
                        val user = document.toObject(com.example.chatapp.data.User::class.java)
                        val intent = Intent(requireContext(), ChatActivity::class.java)
                        intent.putExtra("USER", user)
                        startActivity(intent)
                    }
            }
        }

        binding.chatListRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.chatListRecycler.adapter = adapter

        loadChats()
    }

    private fun loadChats() {
        binding.progressBar.visibility = View.VISIBLE
        listenerRegistration = FirebaseFirestore.getInstance().collection("chats")
            .whereArrayContains("participants", FirebaseAuth.getInstance().currentUser!!.uid)
            .addSnapshotListener { snapshot, e ->
                if (!isAdded || _binding == null) return@addSnapshotListener // Проверка активности фрагмента

                binding.progressBar.visibility = View.GONE
                if (e != null) return@addSnapshotListener

                val chats = snapshot?.documents?.mapNotNull { it.toObject(ChatMetadata::class.java) } ?: emptyList()
                adapter.updateChats(chats)

                binding.emptyText.visibility = if (chats.isEmpty()) View.VISIBLE else View.GONE
                binding.chatListRecycler.visibility = if (chats.isEmpty()) View.GONE else View.VISIBLE
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
        _binding = null
    }
}