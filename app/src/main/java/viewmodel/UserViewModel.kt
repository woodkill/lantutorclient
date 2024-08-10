package com.example.lantutorclient.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UserViewModel : ViewModel() {

    private val _userData = MutableLiveData<MutableMap<String, Any>?>()
    val userData: LiveData<MutableMap<String, Any>?> get() = _userData

    var isUserInitiatedUpdate = false

    fun updateUserData(data: MutableMap<String, Any>?) {
        _userData.value = data
    }
}
