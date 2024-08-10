package com.example.lantutorclient

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.lantutorclient.databinding.ActivityMainBinding
import com.google.firebase.firestore.FirebaseFirestore
import androidx.activity.viewModels
import com.example.lantutorclient.viewmodel.UserViewModel
import com.google.firebase.firestore.Query

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
        if (userViewModel.userData.value == null) {
            fetchUserData()
        }

        // 프래그먼트 초기 설정
        replaceFragment(Setting())
        // 프로그램적으로 'setting' 아이템을 선택 상태로 설정
        binding.bottomNavigationView.selectedItemId = R.id.setting

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
                R.id.setting -> replaceFragment(Setting())
                else -> replaceFragment(Setting())
            }
        }

    }

    private fun fetchUserData() {
        // Firestore에서 사용자 정보 가져오기
        db.collection(COL_USERS)
            .orderBy(KEY_CREATED_AT, Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents[0]
                    val user = document.data as MutableMap<String, Any>?
                    user?.set(KEY_DOC_ID, document.id)
                    userViewModel.updateUserData(document.data as MutableMap<String, Any>?)
                } else {
                    Log.d("MainActivity", "No such document")
                }
            }
            .addOnFailureListener { exception ->
                userViewModel.updateUserData(
                    mutableMapOf(
                        KEY_NAME to "",
                        KEY_AGE to "",
                        KEY_NATIVE_LANG to "",
                        KEY_LEARN_LANG to "",
                        KEY_LEVEL to ""
                    )
                )
                Log.d("MainActivity", "get failed with ", exception)
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