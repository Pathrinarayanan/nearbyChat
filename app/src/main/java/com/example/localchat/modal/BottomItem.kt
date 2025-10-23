package com.example.mychat.modal

import androidx.annotation.DrawableRes

data class BottomItem(
    val title : String,
    @DrawableRes val img : Int,
    @DrawableRes val selectedImg : Int
)
