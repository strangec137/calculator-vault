package com.example.data

import kotlinx.coroutines.flow.Flow

class PasswordRepository(private val passwordDao: PasswordDao) {
    val allPasswords: Flow<List<PasswordEntry>> = passwordDao.getAllPasswords()

    suspend fun insert(entry: PasswordEntry): Long {
        return passwordDao.insertPassword(entry)
    }

    suspend fun update(entry: PasswordEntry) {
        passwordDao.updatePassword(entry)
    }

    suspend fun delete(entry: PasswordEntry) {
        passwordDao.deletePassword(entry)
    }

    suspend fun deleteById(id: Int) {
        passwordDao.deletePasswordById(id)
    }
}
