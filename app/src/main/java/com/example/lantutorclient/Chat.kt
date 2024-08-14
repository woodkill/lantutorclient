package com.example.lantutorclient

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lantutorclient.databinding.FragmentChatBinding
import com.example.lantutorclient.viewmodel.UserViewModel
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions

/**
 * A simple [Fragment] subclass.
 * Use the [Chat.newInstance] factory method to
 * create an instance of this fragment.
 */
class Chat : Fragment() {

    private lateinit var binding: FragmentChatBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessageAdapter
    private val messageList = ArrayList<Message>()
    private var listenerRegistration: ListenerRegistration? = null

    private lateinit var db: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions

    private var previousChatThreadId: String? = null

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
        binding = FragmentChatBinding.inflate(inflater, container, false)

        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        recyclerView = binding.rvChat
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
            val currentChatThreadId = userData?.get(KEY_CHAT_THREAD_ID) as? String
            // 이전의 chatThreadId와 비교하여 변경되었을 때만 실행
            if (currentChatThreadId != previousChatThreadId) {
                previousChatThreadId = currentChatThreadId
                // Chat 프래그먼트 대화내용 리셋
                val chatFragment = parentFragmentManager.findFragmentById(R.id.chatFragmentContainer) as? Chat
                chatFragment?.restartListening()
            }
        })

        // TODO:editText 클릭하면 recyclerview 스크롤다운하는 코드 넣자.

        // Send 버튼 클릭 리스너 설정
        binding.sendChatBtn.setOnClickListener {

            val rootView = binding.root // 또는 requireView()
            val focusedView = rootView.findFocus()
            if (focusedView is EditText) {
                focusedView.clearFocus()
                val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                // 현재 포커스가 있는 뷰에서 키보드를 숨깁니다.
                imm.hideSoftInputFromWindow(activity?.window?.decorView?.rootView?.windowToken, 0)
            }

            // userDocId
            var userDocId = userViewModel.userData.value?.get(KEY_DOC_ID) as? String ?: ""
            if (userDocId.isEmpty())
                return@setOnClickListener
            // 입력한 메세지
            val chatContent = binding.etChatContent.text.toString().trim()
            if (chatContent.isEmpty())
                return@setOnClickListener

            // 먼저 사용자의 메세지를 firestore에 기록
            val userMessage_ref = db.collection(COL_USERS).document(userDocId).collection(COL_CHAT).document()
            val userMessage = mutableMapOf<String, Any>(
                KEY_ID to userMessage_ref.id,
                KEY_ROLE to ROLE_USER,
                KEY_MESSAGE to chatContent,
                KEY_CREATED_AT to com.google.firebase.Timestamp.now()  // Firestore에서 현재 타임스탬프 추가
            )
            userMessage_ref.set(userMessage)
                .addOnSuccessListener { docRef ->
                    binding.etChatContent.text.clear()
                    println("DocumentSnapshot successfully added with ID: $userMessage_ref.id")
                    // firestore에 대화 메세지 하나 삽입 성공
                    // AI의 응답을 얻기 위한 cloud function을 호출.
                    callCloudFunctionUserMessage(chatContent)
                }
                .addOnFailureListener { e ->
                    println("Error updating document: $e")
                }
        }

        // binding.root를 반환하여 뷰 바인딩을 연결합니다.
        return binding.root

        // Inflate the layout for this fragment
        // return inflater.inflate(R.layout.fragment_chat, container, false)
    }


    // 외부에서 리스너를 다시 시작하는 메서드
    fun restartListening() {
        listenToCollectionChanges()
    }


    private fun listenToCollectionChanges() {

        // 먼저 userDocId
        var userDocId = userViewModel.userData.value?.get(KEY_DOC_ID) as? String ?: ""
        if (userDocId.isEmpty())
            return

        // 기존 리스너 제거
        listenerRegistration?.remove()

        // RecyclerView 내용을 초기화 (기존의 메시지 목록을 지우고 새로 추가)
        messageList.clear()
        adapter.notifyDataSetChanged()

        // 새로운 컬렉션에 대한 리스너 설정
        listenerRegistration = db.collection(COL_USERS)
            .document(userDocId)
            .collection(COL_CHAT)
            .orderBy(KEY_CREATED_AT, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("Chat Fragment", "chat Collection Listen failed.", e)
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

    private fun callCloudFunctionUserMessage(chatMessage: String) {

        // thread 등의 정보가 있어야 한다.
        val data = userViewModel.userData.value
        // Cloud Function 호출
        data?.let {
            val userDocId = it[KEY_DOC_ID] as String
            val chat_thread_id = it[KEY_CHAT_THREAD_ID] as String
            val sendData = hashMapOf<String, String>()
            sendData[KEY_LEARN_TYPE] = KEY_CHAT
            sendData[KEY_DOC_ID] = userDocId
            sendData[KEY_THREAD_ID] = chat_thread_id
            sendData[KEY_MESSAGE] = chatMessage
            functions
                .getHttpsCallable("on_request_user_message")
                .call(sendData)
                .addOnSuccessListener { result ->
                    // Cloud Function에서 반환된 응답 처리
                    val response = result.data as Map<*, *>
                    Log.d("Chat", response[KEY_RESULT] as String)
                }
                .addOnFailureListener { e ->
                    Log.e("Chat", "Cloud Function failed", e)
                }
        } ?: run {
            Log.e("Chat", "No data available in userViewModel.userData")
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment Chat.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() = Chat()
    }
}