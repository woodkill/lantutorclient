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

    // 새로운 키-값 업데이트 메서드
    fun updateUserDataField(key: String, value: Any) {
        // 현재 userData 값을 가져오거나 없으면 새로운 MutableMap을 생성
        val currentData = _userData.value ?: mutableMapOf()

        // MutableMap에 키-값을 추가 또는 업데이트
        currentData[key] = value

        // 업데이트된 데이터를 다시 LiveData에 설정
        _userData.value = currentData
    }
}
