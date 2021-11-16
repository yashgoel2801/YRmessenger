package yash_g.ritik_k.YR_messenger

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import yash_g.ritik_k.YR_messenger.databinding.ActivitySetUpProfileBinding
import yash_g.ritik_k.YR_messenger.model.User
import java.net.URI

class SetUpProfileActivity : AppCompatActivity() {
    private val RESULT_CODE =2
    private lateinit var binding:ActivitySetUpProfileBinding
    private lateinit var auth:FirebaseAuth
    private lateinit var storage:FirebaseStorage
    private lateinit var DBref: FirebaseFirestore
    private lateinit var selectedImg:Uri
    private lateinit var dialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetUpProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        DBref = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        auth = Firebase.auth

        dialog = ProgressDialog(this)
        dialog!!.setMessage("Updating Profile...")
        dialog!!.setCancelable(false)


        binding.imageView.setOnClickListener {
            val intent= Intent()
            intent.apply {
                action=Intent.ACTION_GET_CONTENT
                type="image/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg","image/png"))
                startActivityForResult(intent,RESULT_CODE)
            }
        }
        binding.continueBtn.setOnClickListener{
            val name:String= binding.nameBox.text.toString()
            if(name.isNullOrBlank()){
                binding.nameBox.setError("please Enter your name")
                return@setOnClickListener
            }
            dialog!!.show()
            if(selectedImg!=null){
                val storageRef:StorageReference = storage.getReference().child("Profiles").child(auth.uid!!)
                storageRef.putFile(selectedImg).addOnCompleteListener{task->
                    if(task.isSuccessful){
                        storageRef.downloadUrl.addOnSuccessListener {  uri->
                            val imageUrl:String=uri.toString()
                            val user:User=User(auth.uid.toString()!!,binding.nameBox.text.toString()?:"",auth.currentUser!!.phoneNumber.toString(),imageUrl)
                            DBref.collection("users")
                                .document(auth.uid!!).set(user).addOnSuccessListener {
                                    dialog.dismiss()
                                    val intent=Intent(this,MainActivity::class.java)
                                    startActivity(intent)
                                    finish();
                                }

                        }
                    }
                }
            }else{
                val user:User=User(auth.uid.toString()!!,binding.nameBox.text.toString()?:"","sdasd","No Image")
                DBref.collection("users")
                    .document(auth.uid!!).set(user).addOnSuccessListener {
                        dialog.dismiss()
                        val intent=Intent(this,MainActivity::class.java)
                        startActivity(intent)
                        finish();
                    }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(RESULT_CODE==requestCode && resultCode == Activity.RESULT_OK && data!=null){
            if(data.data!=null){
                binding.imageView.setImageURI(data.data!!)
                selectedImg =data.data!!
            }
        }
    }
}