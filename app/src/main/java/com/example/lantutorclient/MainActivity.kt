package com.example.lantutorclient

import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputBinding
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.lantutorclient.databinding.ActivityMainBinding
import com.google.firebase.firestore.FirebaseFirestore
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import com.example.lantutorclient.viewmodel.UserViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: FirebaseFirestore
    private val userViewModel: UserViewModel by viewModels()  // ViewModel 초기화

    private var cachedUserData: Map<String, Any>? = null  // 사용자 데이터 캐시

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firestore 인스턴스 초기화
        db = FirebaseFirestore.getInstance()

        // Firestore에서 사용자 정보 가져오기
        if (userViewModel.userData == null) {
            fetchUserData()
        } else {
            replaceFragmentWithCachedData()
        }

        // 프래그먼트 초기 설정
        replaceFragment(Setting())

//        enableEdgeToEdge()
//        setContentView(R.layout.activity_main)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }

        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.chat -> replaceFragment(Chat())
                R.id.edit -> replaceFragment(Edit())
                R.id.quiz -> replaceFragment(Quiz())
                R.id.setting -> {
                    replaceFragmentWithCachedData()
                    true
                }
                else -> replaceFragment(Setting())
            }
        }

    }

    private fun fetchUserData() {
        // Firestore에서 사용자 정보 가져오기
        db.collection("users").document("SINGLE_USER_DOCUMENT_ID").get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    userViewModel.userData = document.data
                    replaceFragmentWithCachedData()
                }
            }
            .addOnFailureListener { exception ->
                Log.d("MainActivity", "get failed with ", exception)
            }
    }

    private fun replaceFragmentWithCachedData() {
        userViewModel.userData?.let { userData ->
            val settingFragment = Setting.newInstance(
                userData["name"] as? String ?: "",
                userData["age"] as? String ?: "",
                userData["nativeLang"] as? String ?: "",
                userData["learnLang"] as? String ?: "",
                userData["level"] as? String ?: ""
            )
            replaceFragment(settingFragment)
        }
    }

    private fun replaceFragment(fragment: Fragment): Boolean {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayout, fragment)
        fragmentTransaction.commit()
        return true
    }


}