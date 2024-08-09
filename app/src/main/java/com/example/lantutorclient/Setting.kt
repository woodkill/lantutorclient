package com.example.lantutorclient

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.lantutorclient.databinding.FragmentSettingBinding
import com.example.lantutorclient.viewmodel.UserViewModel
import com.google.firebase.firestore.FirebaseFirestore


/**
 * A simple [Fragment] subclass.
 * Use the [Setting.newInstance] factory method to
 * create an instance of this fragment.
 */
class Setting : Fragment() {

    private var name: String? = null
    private var age: String? = null
    private var nativeLang: String? = null
    private var learnLang: String? = null
    private var level: String? = null

    private lateinit var binding: FragmentSettingBinding
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            name = it.getString(ARG_NAME)
            age = it.getString(ARG_AGE)
            nativeLang = it.getString(ARG_NATIVE_LANG)
            learnLang = it.getString(ARG_LEARN_LANG)
            level = it.getString(ARG_LEVEL)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // View 바인딩 초기화
        binding = FragmentSettingBinding.inflate(inflater, container, false)

        // Firestore 인스턴스 초기화
        db = FirebaseFirestore.getInstance()

        // ViewModel 초기화 (Activity와 공유)
        val userViewModel: UserViewModel by activityViewModels()

        // Firestore에서 불러온 데이터를 EditText에 채우기
        userViewModel.userData?.let { userData ->
            binding.etName.setText(userData["name"] as? String ?: "")
            binding.etAge.setText(userData["age"] as? String ?: "")
            binding.etNativeLang.setText(userData["nativeLang"] as? String ?: "")
            binding.etLearnLang.setText(userData["learnLang"] as? String ?: "")
            binding.etLevel.setText(userData["level"] as? String ?: "")
        }

        // 저장 버튼 클릭 리스너 설정
        binding.saveSettingButton.setOnClickListener {
            val name = binding.etName.text.toString()
            val age = binding.etAge.text.toString()
            val nativeLang = binding.etNativeLang.text.toString()
            val learnLang = binding.etLearnLang.text.toString()
            val level = binding.etLevel.text.toString()
            val user = hashMapOf(
                "name" to name,
                "age" to age,
                "nativeLang" to nativeLang,
                "learnLang" to learnLang,
                "level" to level
            )
            // Firestore에 데이터 저장 및 ViewModel 업데이트
            db.collection("users").document("SINGLE_USER_DOCUMENT_ID")  // 이미 존재하는 문서 ID 사용
                .set(user)
                .addOnSuccessListener {
                    println("DocumentSnapshot successfully updated!")

                    // ViewModel 업데이트
                    userViewModel.userData = user
                }
                .addOnFailureListener { e ->
                    println("Error updating document: $e")
                }
        }

        // binding.root를 반환하여 뷰 바인딩을 연결합니다.
        return binding.root

        // Inflate the layout for this fragment
        // return inflater.inflate(R.layout.fragment_setting, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @return A new instance of fragment Setting.
         */
        private const val ARG_NAME = "name"
        private const val ARG_AGE = "age"
        private const val ARG_NATIVE_LANG = "nativeLang"
        private const val ARG_LEARN_LANG = "learnLang"
        private const val ARG_LEVEL = "level"

        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(name: String, age: String, nativeLang: String, learnLang: String, level: String) =
            Setting().apply {
                arguments = Bundle().apply {
                    putString(ARG_NAME, name)
                    putString(ARG_AGE, age)
                    putString(ARG_NATIVE_LANG, nativeLang)
                    putString(ARG_LEARN_LANG, learnLang)
                    putString(ARG_LEVEL, level)
                }
            }
    }
}