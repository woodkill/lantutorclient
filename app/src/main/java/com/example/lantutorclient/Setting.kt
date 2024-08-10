package com.example.lantutorclient

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.example.lantutorclient.databinding.FragmentSettingBinding
import com.example.lantutorclient.*
import com.example.lantutorclient.viewmodel.UserViewModel
import com.google.firebase.firestore.FirebaseFirestore


/**
 * A simple [Fragment] subclass.
 * Use the [Setting.newInstance] factory method to
 * create an instance of this fragment.
 */
class Setting : Fragment() {

    private lateinit var binding: FragmentSettingBinding
    private lateinit var db: FirebaseFirestore

    // ViewModel 초기화 (Activity와 공유)
    val userViewModel: UserViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // View 바인딩 초기화
        binding = FragmentSettingBinding.inflate(inflater, container, false)

        // Firestore 인스턴스 초기화
        db = FirebaseFirestore.getInstance()

        userViewModel.userData.observe(viewLifecycleOwner, Observer { userData ->
            if (userViewModel.isUserInitiatedUpdate) {
                // 사용자가 데이터를 직접 저장한 경우, UI 갱신을 생략
                userViewModel.isUserInitiatedUpdate = false
                return@Observer
            }
            // UI 갱신
            userData?.let {
                binding.etName.setText(it[KEY_NAME] as? String ?: "")
                binding.etAge.setText(it[KEY_AGE] as? String ?: "")
                binding.etNativeLang.setText(it[KEY_NATIVE_LANG] as? String ?: "")
                binding.etLearnLang.setText(it[KEY_LEARN_LANG] as? String ?: "")
                binding.etLevel.setText(it[KEY_LEVEL] as? String ?: "")
            }
        })

        // 저장 버튼 클릭 리스너 설정
        binding.saveSettingButton.setOnClickListener {
            // UI에서 값을 가져와서
            val name = binding.etName.text.toString()
            val age = binding.etAge.text.toString()
            val nativeLang = binding.etNativeLang.text.toString()
            val learnLang = binding.etLearnLang.text.toString()
            val level = binding.etLevel.text.toString()

            val user = mutableMapOf<String, Any>(
                KEY_NAME to name,
                KEY_AGE to age,
                KEY_NATIVE_LANG to nativeLang,
                KEY_LEARN_LANG to learnLang,
                KEY_LEVEL to level,
                KEY_CREATED_AT to com.google.firebase.Timestamp.now()  // Firestore에서 현재 타임스탬프 추가
            )

            // 사용자가 데이터를 업데이트했음을 알리는 플래그 설정
            userViewModel.isUserInitiatedUpdate = true

            // Firestore에 데이터 저장 및 ViewModel 업데이트
            db.collection(COL_USERS)
                .add(user)
                .addOnSuccessListener { docRef ->
                    println("DocumentSnapshot successfully added with ID: $docRef.id")
                    // ViewModel 업데이트
                    user[KEY_DOC_ID] = docRef.id
                    userViewModel.updateUserData(user)
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
         * @return A new instance of fragment Setting.
         */

        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() = Setting()
    }
}