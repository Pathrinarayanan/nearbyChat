package com.example.mychat.modal

data class User(
    val uid : String = "",
    val email : String = "",
    val name  : String  = "",
    val profileImage : String  = "",
    val createdAt : Long =0L,
    val isOnline : Boolean = false,
    val lastSeen : Long = 0L,
    val latitude : Double = 0.0,
    val longitude : Double = 0.0
)
