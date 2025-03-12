package com.example.myapplication.models

object AppMode {
    private var onlineMode = true
    fun isOnlineMode() = onlineMode
    fun setOnlineMode(mode: Boolean) {
        onlineMode = mode
    }
}
