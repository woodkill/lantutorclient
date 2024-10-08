package com.example.lantutorclient

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

/**
 * A simple [Fragment] subclass.
 * Use the [Sett    ing.newInstance] factory method to
 * create an instance of this fragment.
 */
class Setting : Fragment() {

    private lateinit var binding: FragmentSettingBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessageAdapter
    private val messageList = ArrayList<Message>()
    private var listenerRegistration: ListenerRegistration? = null

    private lateinit var db: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions

    // ViewModel 초기화 (Activity와 공유)
    val userViewModel: UserViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // View 바인딩 초기화
        binding = FragmentSettingBinding.inflate(inflater, container, false)

        val view = inflater.inflate(R.layout.fragment_setting, container, false)
        recyclerView = binding.rvSettingMessage
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = MessageAdapter(requireContext(), messageList)
        recyclerView.adapter = adapter

        // Firestore 인스턴스 초기화
        db = FirebaseFirestore.getInstance()

        // Firebase Functions 초기화
        functions = FirebaseFunctions.getInstance()
        //functions.useEmulator("127.0.0.1", 5001)

        // 초기 컬렉션 리스닝 설정
        listenToCollectionChanges()

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

        // 새로시작 버튼 클릭 리스너 설정
        binding.startNewBtn.setOnClickListener {

            val rootView = binding.root // 또는 requireView()
            val focusedView = rootView.findFocus()
            if (focusedView is EditText) {
                focusedView.clearFocus()
                val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                // 현재 포커스가 있는 뷰에서 키보드를 숨깁니다.
                imm.hideSoftInputFromWindow(activity?.window?.decorView?.rootView?.windowToken, 0)
            }

            // UI에서 값을 가져와서
            val name = binding.etName.text.toString()
            val age = binding.etAge.text.toString()
            val nativeLang = binding.etNativeLang.text.toString()
            val learnLang = binding.etLearnLang.text.toString()
            val level = binding.etLevel.text.toString()

            // 아래 내용들은 AI Assistant에게 알려줄 학생의 정보.
            // 학생의 정보를 새로 알려주는 것은 새로 학습을 시작하겠다는 의미
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
            val userDoc_ref = db.collection(COL_USERS).document()
            user[KEY_DOC_ID] = userDoc_ref.id
            userDoc_ref.set(user)
                .addOnSuccessListener { docRef ->
                    println("DocumentSnapshot successfully added with ID: $docRef.id")
                    // firestore에 새로 시작한 학생의 정보를 새로 만드는 것 성공.
                    // ViewModel 업데이트
                    userViewModel.updateUserData(user)
                    // 새로운 학습 시작을 위한 cloud function을 호출.
                    callCloudFunctionStartNew()
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

    private fun listenToCollectionChanges() {

        // 기존 리스너 제거
        listenerRegistration?.remove()

        // 새로운 컬렉션에 대한 리스너 설정
        listenerRegistration = db.collection(COL_SETTINGS)
            .orderBy(KEY_CREATED_AT, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("Setting Fragment", "Setting Collection Listen failed.", e)
                    return@addSnapshotListener
                }

                for (dc in snapshots!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> {
                            val newMessage = dc.document.toObject(Message::class.java)
                            adapter.addMessage(newMessage)
                        }
                        DocumentChange.Type.MODIFIED -> {
                            val updatedMessage = dc.document.toObject(Message::class.java)
                            adapter.updateMessage(updatedMessage)
                        }
                        DocumentChange.Type.REMOVED -> {
                            adapter.removeMessage(dc.document.id)
                        }
                    }
                }
                recyclerView.scrollToPosition(adapter.itemCount - 1)
            }
    }

    private fun callCloudFunctionStartNew() {

        // userViewModel.userData 를 그냥 보낸다.
        val data = userViewModel.userData.value
        // Cloud Function 호출
        data?.let {
            // createdAt 은 제외
            val mutableData = it.toMutableMap()
            mutableData.remove(KEY_CREATED_AT)
            functions
                .getHttpsCallable("on_request_start_new")
                .call(mutableData)
                .addOnSuccessListener { result ->
                    // Cloud Function에서 반환된 응답 처리
                    val response = result.data as Map<*, *>
                    userViewModel.updateUserDataField(KEY_CHAT_THREAD_ID, response[KEY_CHAT_THREAD_ID] ?: "")
//                    userViewModel.updateUserDataField(KEY_CORR_THREAD_ID, response[KEY_CORR_THREAD_ID] ?: "")
//                    userViewModel.updateUserDataField(KEY_QUIZ_THREAD_ID, response[KEY_QUIZ_THREAD_ID] ?: "")

                    Log.d("Setting", "Response from Cloud Function")
                    Log.d("Setting", "$KEY_CHAT_THREAD_ID: $response[KEY_CHAT_THREAD_ID]")
                    Log.d("Setting", "$KEY_CORR_THREAD_ID: $response[KEY_CORR_THREAD_ID]")
                    Log.d("Setting", "$KEY_QUIZ_THREAD_ID: $response[KEY_QUIZ_THREAD_ID]")
                }
                .addOnFailureListener { e ->
                    Log.e("Setting", "Cloud Function failed", e)
                }
        } ?: run {
            Log.e("Setting", "No data available in userViewModel.userData")
        }
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