package yash_g.ritik_k.YR_messenger

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import yash_g.ritik_k.YR_messenger.adapters.MessagesAdapter
import yash_g.ritik_k.YR_messenger.databinding.ActivityChatBinding
import yash_g.ritik_k.YR_messenger.model.Message
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: MessagesAdapter
    private lateinit var messagesArray: ArrayList<Message>
    private lateinit var SenderRoom: String
    private lateinit var ReceiverRoom: String
    private lateinit var DBref: FirebaseFirestore


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val name: String? = intent.getStringExtra("name")
        val receiverUid: String? = intent.getStringExtra("uid")
        setSupportActionBar(binding!!.toolbar)
        binding.name.setText(name)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        SenderRoom = Firebase.auth.uid!! + receiverUid
        ReceiverRoom = receiverUid + Firebase.auth.uid!!

        messagesArray = ArrayList()
        messagesArray.sortBy {
            it.gettimestamp()
        }
        adapter = MessagesAdapter(this, messages = messagesArray,senderRoom = SenderRoom,receiverRoom=ReceiverRoom)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        DBref = FirebaseFirestore.getInstance()

        DBref.collection("chats").document(SenderRoom)
            .collection("messages").get()
            .addOnSuccessListener {
                messagesArray.clear()
                for (document in it) {
                    val message: Message = document.toObject(Message::class.java)
                    messagesArray.add(message!!)
                    messagesArray.sortBy {
                        it.gettimestamp()
                    }
                }
                adapter.notifyDataSetChanged()
                binding.recyclerView.scrollToPosition(binding.recyclerView.adapter!!.itemCount -1)

            }

        listenForMessages()
        binding.sendBtn.setOnClickListener {
            val msgTxt: String = binding.messageBox.text.toString()
            val message: Message = Message(msgTxt, Firebase.auth.uid!!, Date().time)
            val randomKey = DBref.collection("randomkeys").document().id

            val lastMsgMap:HashMap<String,Any> = HashMap()
            lastMsgMap.put("lastMsg",message.getmessage())
            lastMsgMap.put("lastMsgTime",Date().time)
            DBref.collection("chats").document(SenderRoom).collection("lastMsgs")
                .document("lastmsg").set(lastMsgMap,SetOptions.merge())
            DBref.collection("chats").document(ReceiverRoom).collection("lastMsgs")
                .document("lastmsg").set(lastMsgMap,SetOptions.merge())



            message.setmessageId(randomKey)
            binding.messageBox.setText("")
            DBref.collection("chats").document(SenderRoom)
                .collection("messages").document(randomKey).set(message)
                .addOnSuccessListener { }
            DBref.collection("chats").document(ReceiverRoom)
                .collection("messages").document(randomKey).set(message)
        }
    }

    private fun listenForMessages() {
        val DBref = FirebaseFirestore.getInstance().collection("chats")
            .document(SenderRoom).collection("messages")
            .addSnapshotListener { querySnapshot, error ->
                error?.let {
                    Toast.makeText(this,"asdasds",Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                querySnapshot?.let{
                    messagesArray.clear()
                    for(document in it.documents){
                        val message: Message? = document.toObject(Message::class.java)
                        messagesArray.add(message!!)
                        messagesArray.sortBy {
                            it.gettimestamp()
                        }
                        Log.e("error ddd","${document.data}")
                        adapter.notifyDataSetChanged()
                        binding.recyclerView.scrollToPosition(binding.recyclerView.adapter!!.itemCount -1)
                    }
                }

        }
    }
}
