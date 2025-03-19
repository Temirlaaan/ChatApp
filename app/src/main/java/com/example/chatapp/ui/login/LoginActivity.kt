package com.example.chatapp.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chatapp.MainActivity
import com.example.chatapp.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding.usernameInput.visibility = View.GONE
        binding.registerButton.visibility = View.GONE

        binding.loginButton.setOnClickListener { login() }
        binding.registerButton.setOnClickListener { register() }
        binding.toggleButton.setOnClickListener { toggleMode() }
    }

    private fun toggleMode() {
        isLoginMode = !isLoginMode
        binding.usernameInput.visibility = if (isLoginMode) View.GONE else View.VISIBLE
        binding.loginButton.visibility = if (isLoginMode) View.VISIBLE else View.GONE
        binding.registerButton.visibility = if (isLoginMode) View.GONE else View.VISIBLE
        binding.toggleButton.text = if (isLoginMode) "Need an account?" else "Already have an account?"
    }

    private fun login() {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                updateUserStatus(true)
                binding.progressBar.visibility = View.GONE
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun register() {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()
        val username = binding.usernameInput.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        // Проверка уникальности имени пользователя
        db.runTransaction { transaction ->
            val usernameRef = db.collection("usernames").document(username)
            val snapshot = transaction.get(usernameRef)
            if (snapshot.exists()) {
                throw Exception("Username already taken")
            }
            // Резервируем имя пользователя
            transaction.set(usernameRef, mapOf("uid" to "", "owner" to email))
            null
        }.addOnSuccessListener {
            // Регистрация пользователя
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val userId = result.user!!.uid
                    val user = hashMapOf(
                        "uid" to userId,
                        "email" to email,
                        "username" to username,
                        "isOnline" to true,
                        "lastActive" to System.currentTimeMillis()
                    )
                    // Сохранение данных пользователя
                    db.collection("users").document(userId)
                        .set(user)
                        .addOnSuccessListener {
                            // Обновляем запись usernames с правильным uid
                            db.collection("usernames").document(username)
                                .update("uid", userId)
                                .addOnSuccessListener {
                                    binding.progressBar.visibility = View.GONE
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    binding.progressBar.visibility = View.GONE
                                    Toast.makeText(this, "Failed to update username: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(this, "Failed to save user: ${e.message}", Toast.LENGTH_SHORT).show()
                            db.collection("usernames").document(username).delete()
                        }
                }
                .addOnFailureListener { e ->
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    db.collection("usernames").document(username).delete()
                }
        }.addOnFailureListener { e ->
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUserStatus(isOnline: Boolean) {
        auth.currentUser?.let {
            db.collection("users").document(it.uid)
                .update(
                    mapOf(
                        "isOnline" to isOnline,
                        "lastActive" to System.currentTimeMillis()
                    )
                )
        }
    }
}