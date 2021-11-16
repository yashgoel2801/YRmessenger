package yash_g.ritik_k.YR_messenger

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import yash_g.ritik_k.YR_messenger.adapters.TopStatusAdapter
import yash_g.ritik_k.YR_messenger.adapters.UsersAdapter
import yash_g.ritik_k.YR_messenger.databinding.ActivityMainBinding
import yash_g.ritik_k.YR_messenger.model.Status
import yash_g.ritik_k.YR_messenger.model.User
import yash_g.ritik_k.YR_messenger.model.UserStatus
import java.lang.Error
import java.lang.Exception
import java.lang.NullPointerException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {
    private lateinit var binding:ActivityMainBinding
    private lateinit var DBref: FirebaseFirestore
    private lateinit var usersArray:ArrayList<User>
    private lateinit var statusArray:ArrayList<UserStatus>
    private lateinit var usersAdapter:UsersAdapter
    private lateinit var TopStatusAdapter:TopStatusAdapter
    private lateinit var storage: FirebaseStorage
    private lateinit var progressDialog: ProgressDialog
    private lateinit var user:User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        DBref = FirebaseFirestore.getInstance()
        DBref.collection("users").document(Firebase.auth.uid!!)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("TAG", "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                   user = snapshot.toObject(User::class.java)!!
                    Log.d("TAG", "Current data: ${snapshot.data}")
                } else {
                    Log.d("TAG", "Current data: null")
                }

            }


        usersArray = ArrayList()
        statusArray = ArrayList()

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("uploading image")
        progressDialog.setCancelable(false)

        usersAdapter = UsersAdapter(mContext = this,mUsers=usersArray)
        TopStatusAdapter = TopStatusAdapter(mContext = this,statusList = statusArray)
        val statusListLayoutManager=LinearLayoutManager(this)
        statusListLayoutManager.setOrientation(RecyclerView.HORIZONTAL)
        binding.recyclerView.adapter= usersAdapter
        binding.statusList.apply {
            adapter = TopStatusAdapter
            layoutManager = statusListLayoutManager
        }

        DBref.collection("stories").addSnapshotListener {snapshot, e ->
        if (e != null) {
                Log.w("TAG", "Listen failed.", e)
                return@addSnapshotListener
            }
            if (snapshot != null && !(snapshot.getDocumentChanges().isEmpty())) {
                for(snap in snapshot.getDocumentChanges()){
                    val uStatus:UserStatus = UserStatus()
                    uStatus.apply {
                        setname(snap!!.document.get("name").toString())
                        setlastupdated(snap!!.document.getLong("lastupdated")!!)
                        setprofileimage(snap!!.document.get("profileImage").toString())

                    }
                    val statuesArray=ArrayList<Status>()
                    DBref.collection("stories").document(snap.document.id).collection("statues")
                        .addSnapshotListener { value, error ->
                            for (status in value!!.documents){
                                val tempStatus = status.toObject(Status::class.java)!!
                                statuesArray.add(tempStatus)
                            }
                            uStatus.setstatusList(statuesArray)
                        }
                    statusArray.add(uStatus)
                }
                TopStatusAdapter.notifyDataSetChanged()
            } else {
                Log.d("TAG", "Current data: null")
            }

        }

        binding.bottomNavigationView.setOnNavigationItemSelectedListener {
             when(it.itemId){
                 R.id.status-> {
                     val intent: Intent = Intent()
                     intent.setType("image/*")
                     intent.setAction(Intent.ACTION_GET_CONTENT)
                     startActivityForResult(intent,75)
                     return@setOnNavigationItemSelectedListener true
                 }
                R.id.calls->{
                    TODO()  }
                R.id.chats->{ TODO()}
                else->throw AssertionError()
             }
        }

        DBref.collection("users").get().addOnSuccessListener {
              (usersArray as ArrayList<User>).clear()
              for(snapshot in it){

                    val user:User =snapshot.toObject(User::class.java)
                    if(!(user.uid.equals(Firebase.auth.currentUser!!.uid)))
                        (usersArray as ArrayList<User>).add(user!!)


              }
              usersAdapter.notifyDataSetChanged()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(75==requestCode && resultCode == Activity.RESULT_OK && data!=null){
            if(data.data!=null){
                progressDialog.show()
                storage= FirebaseStorage.getInstance()
                val storageRef =storage.getReference().child("status")
                    .child(Date().time.toString()+"")
                storageRef.putFile(data.data!!).addOnCompleteListener{
                    storageRef.downloadUrl.addOnSuccessListener {
                        val userStatusObj=UserStatus()
                        userStatusObj.setname(user.name)
                        userStatusObj.setprofileimage(user.profileImage)
                        userStatusObj.setlastupdated(Date().time)
                        val userStatusMap:HashMap<String,Any?> = HashMap()
                        userStatusMap.put("name",userStatusObj.getname())
                        userStatusMap.put("profileImage",userStatusObj.getprofileimage())
                        userStatusMap.put("lastupdated",userStatusObj.getlastupdated())
                        val statusObj =Status(it.toString(),userStatusObj.getlastupdated())
                        DBref.collection("stories")
                            .document(Firebase.auth.uid!!).set(userStatusMap, SetOptions.merge())
                        DBref.collection("stories")
                            .document(Firebase.auth.uid!!).collection("statues")
                            .document().set(statusObj)
                        progressDialog.dismiss()
                    }
                }
            }
        }
    }


    override fun onCreateOptionsMenu(menu:Menu):Boolean{
        menuInflater.inflate(R.menu.topmenu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        TODO("show fragments on click ")
        when(item.itemId){
            R.id.search->
                Toast.makeText(this,"search clicked",Toast.LENGTH_SHORT).show()
            R.id.group->
                Toast.makeText(this,"group clicked",Toast.LENGTH_SHORT).show()
            R.id.invite->
                Toast.makeText(this,"invite clicked",Toast.LENGTH_SHORT).show()
            R.id.settings->
                Toast.makeText(this,"settings clicked",Toast.LENGTH_SHORT).show()

        }
        return super.onOptionsItemSelected(item)
    }
}