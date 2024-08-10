package com.example.lantutorclient

import com.example.lantutorclient.Message
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private val context: Context, private val messageList: ArrayList<Message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val byUser = 1 // 사용자가 쓴 메세지
        private val byAssistant = 2 // AI가 쓴 메세지 타입

    class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sentMessage: TextView = itemView.findViewById(R.id.tvSentMessage)
    }

    class ReceiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiveMessage: TextView = itemView.findViewById(R.id.tvReceiveMessage)
    }

    override fun getItemViewType(position: Int): Int {
        val currentMessage: Message = messageList[position]
        return currentMessage.byWho
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if(viewType == byAssistant) { // 받은 메세지
            val view: View = LayoutInflater.from(context).inflate(R.layout.receive_message, parent, false)
            ReceiveViewHolder(view)
        } else {
            val view: View = LayoutInflater.from(context).inflate(R.layout.sent_message, parent, false)
            SentViewHolder(view)
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMessage = messageList[position]
        if (currentMessage.byWho == byAssistant) {
            val viewHolder = holder as ReceiveViewHolder
            viewHolder.receiveMessage.text = currentMessage.message.toString()
        } else {
            val viewHolder = holder as SentViewHolder
            viewHolder.sentMessage.text = currentMessage.message.toString()
        }
    }

}