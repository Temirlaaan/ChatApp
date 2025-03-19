package com.example.chatapp.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatapp.databinding.FragmentUserListBinding
import com.example.chatapp.adapter.UserListAdapter
import com.example.chatapp.data.User
import com.example.chatapp.messages.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class UserListFragment : Fragment() {
    private var _binding: FragmentUserListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: UserListAdapter
    private var listenerRegistration: ListenerRegistration? = null // Для хранения слушателя

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = UserListAdapter { user ->
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("USER", user)
            startActivity(intent)
        }

        binding.userListRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.userListRecycler.adapter = adapter

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filterUsers(s.toString())
            }
        })

        loadUsers()
    }

    private fun loadUsers() {
        binding.progressBar.visibility = View.VISIBLE
        listenerRegistration = FirebaseFirestore.getInstance().collection("users")
            .addSnapshotListener { snapshot, e ->
                if (!isAdded || _binding == null) return@addSnapshotListener // Проверка, что фрагмент активен

                binding.progressBar.visibility = View.GONE
                if (e != null) return@addSnapshotListener

                val users = snapshot?.documents?.mapNotNull { it.toObject(User::class.java) }
                    ?.filter { it.uid != FirebaseAuth.getInstance().currentUser?.uid } ?: emptyList()
                adapter.updateUsers(users)

                binding.emptyText.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
                binding.userListRecycler.visibility = if (users.isEmpty()) View.GONE else View.VISIBLE
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove() // Отмена слушателя
        _binding = null
    }
}