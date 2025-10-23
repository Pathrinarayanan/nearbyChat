package com.example.mychat.modal

sealed class ChatItem{
    data class Header(val date : String) : ChatItem()
    data class Message(val message : ChatMessage) : ChatItem()
}
