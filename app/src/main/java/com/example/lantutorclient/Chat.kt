package com.example.lantutorclient

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lantutorclient.databinding.FragmentChatBinding
import com.example.lantutorclient.viewmodel.UserViewModel
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
    private lateinit var currentCollection: CollectionReference

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
            // Chat 프래그먼트 대화내용 리셋
            val chatFragment = parentFragmentManager.findFragmentById(R.id.chatFragmentContainer) as? Chat
            chatFragment?.restartListening()
        })

        // binding.root를 반환하여 뷰 바인딩을 연결합니다.
        return binding.root

        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_chat, container, false)
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
        listenerRegistration = db.collection(COL_USERS).document(userDocId).collection(COL_CHAT)
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
            }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment Chat.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() = Chat()
    }
}