package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passwords")
data class PasswordEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val platformName: String,
    val emailUsername: String,
    val passwordEncrypted: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
