package com.example.chatapp.messages

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ChatActivityTest {

    @Test
    fun `generateChatId should return the same id regardless of user order`() {
        val chatId1 = generateChatId("userA", "userB")
        val chatId2 = generateChatId("userB", "userA")

        assertThat(chatId1).isEqualTo(chatId2)
        assertThat(chatId1).isEqualTo("userA-userB")
    }

    @Test
    fun `generateChatId should return correct format when users are equal`() {
        val chatId = generateChatId("userX", "userX")

        assertThat(chatId).isEqualTo("userX-userX")
    }

    private fun generateChatId(user1: String, user2: String): String {
        return if (user1 < user2) "$user1-$user2" else "$user2-$user1"
    }
}

