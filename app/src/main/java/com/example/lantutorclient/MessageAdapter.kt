package com.example.lantutorclient

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private val context: Context, private val messageList: ArrayList<Message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sentMessage: TextView = itemView.findViewById(R.id.tvSentMessage)
    }

    class ReceiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiveMessage: TextView = itemView.findViewById(R.id.tvReceiveMessage)
    }

    override fun getItemViewType(position: Int): Int {
        val currentMessage: Message = messageList[position]
        if (currentMessage.role == ROLE_ASSISTATNT)
            return ROLE_TYPE_ASSISTATNT
        else
            return ROLE_TYPE_USER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if(viewType == ROLE_TYPE_ASSISTATNT) { // 받은 메세지
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
        if (currentMessage.role == ROLE_ASSISTATNT) {
            val viewHolder = holder as ReceiveViewHolder
            viewHolder.receiveMessage.text = currentMessage.message.toString()
        } else {
            val viewHolder = holder as SentViewHolder
            viewHolder.sentMessage.text = currentMessage.message.toString()
        }
    }

    fun addMessage(message: Message) {
        messageList.add(message)
        notifyItemInserted(messageList.size - 1)
    }

    fun updateMessage(message: Message) {
        val index = messageList.indexOfFirst { it.id == message.id }
        if (index != -1) {
            messageList[index] = message
            notifyItemChanged(index)
        }
    }

    fun removeMessage(messageId: String) {
        val index = messageList.indexOfFirst { it.id == messageId }
        if (index != -1) {
            messageList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

}