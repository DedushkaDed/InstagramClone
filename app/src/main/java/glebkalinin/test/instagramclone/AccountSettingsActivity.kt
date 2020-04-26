package glebkalinin.test.instagramclone

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.squareup.picasso.Picasso
import com.theartofdev.edmodo.cropper.CropImage
import glebkalinin.test.instagramclone.Model.User
import kotlinx.android.synthetic.main.activity_account_settings.*
import kotlinx.android.synthetic.main.fragment_profile.view.*

class AccountSettingsActivity : AppCompatActivity() {

    private lateinit var firebaseUser: FirebaseUser
    private var checker = ""
    private var myUrl = ""
    private var imageUri: Uri? = null
    private var storageProfilePictureReference: StorageReference? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_settings)


        firebaseUser = FirebaseAuth.getInstance().currentUser!!
        storageProfilePictureReference = FirebaseStorage.getInstance().reference.child("Profile Pictures")


        logout_btn.setOnClickListener {
            FirebaseAuth.getInstance().signOut() //Выход из аккаунта
            val intent = Intent(this, SignInActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        change_image_text_btn.setOnClickListener {
            checker = "clicked"

            CropImage.activity()
                .setAspectRatio(1, 1) // разрешение картинки
                .start(this)
        }

        save_infor_profile_btn.setOnClickListener {
            if (checker == "clicked") {
                uploadImageAndUpdateInfo()
            } else {
                updateUserInfoOnly()
            }
        }


        userInfo()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {

            val result = CropImage.getActivityResult(data)
            imageUri = result.uri
            profile_image_view_profile_frag.setImageURI(imageUri)
        }
        else {
            Toast.makeText(this, "Ошибка,попробуйте снова!", Toast.LENGTH_LONG).show()
        }

    }


    private fun updateUserInfoOnly() {

        when {
            full_name_profile_frag.text.toString() == "" -> {
                Toast.makeText(this, "Пожалуйста,введите имя", Toast.LENGTH_LONG).show()
            }
            username_profile_frag.text.toString() == "" -> {
                Toast.makeText(this, "Пожалуйста,введите логин", Toast.LENGTH_LONG).show()
            }
            bio_profile_frag.text.toString() == "" -> {
                Toast.makeText(
                    this,
                    "Пожалуйста,введите описание своего профиля",
                    Toast.LENGTH_LONG
                ).show()
            }
            else -> {
                val userRef = FirebaseDatabase.getInstance().reference.child("Users")

                val userMap = HashMap<String, Any>() //HashMap -> Загулил.
                userMap["fullname"] = full_name_profile_frag.text.toString().toLowerCase()
                userMap["username"] = username_profile_frag.text.toString().toLowerCase()
                userMap["bio"] = bio_profile_frag.text.toString().toLowerCase()

                //сохранение + обновленных данных
                userRef.child(firebaseUser.uid).updateChildren(userMap)

                Toast.makeText(this, "Информация была успешно обновлена!", Toast.LENGTH_LONG).show()

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }


    private fun userInfo() {
        val userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseUser.uid)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {

                if (p0.exists()) {
                    val user = p0.getValue<User>(User::class.java)

                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.profile)
                        .into(profile_image_view_profile_frag) // Photo пользователя
                    username_profile_frag.setText(user!!.getUsername()) // username
                    full_name_profile_frag.setText(user!!.getFullname()) // fullname
                    bio_profile_frag.setText(user!!.getBio()) // bio

                }
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })
    }


    private fun uploadImageAndUpdateInfo() {


        when {
            imageUri == null -> Toast.makeText(this, "Пожалуйста,выберите фотографию", Toast.LENGTH_LONG).show()

            full_name_profile_frag.text.toString() == "" -> {
                Toast.makeText(this, "Пожалуйста,введите имя", Toast.LENGTH_LONG).show()
            }
            username_profile_frag.text.toString() == "" -> {
                Toast.makeText(this, "Пожалуйста,введите логин", Toast.LENGTH_LONG).show()
            }
            bio_profile_frag.text.toString() == "" -> {
                Toast.makeText(
                    this,
                    "Пожалуйста,введите описание своего профиля",
                    Toast.LENGTH_LONG
                ).show()
            }
            else -> {
                val progressDialog = ProgressDialog(this)
                progressDialog.setTitle("Настройки пользователя")
                progressDialog.setMessage("Пожалуйста подождите,идёт сохранение новых данных...")
                progressDialog.show()

                val fileref = storageProfilePictureReference!!.child(firebaseUser!!.uid + ".jpg") //  Обновление старой фотографии на новую. В этой переменной мы храним фотографию пользователя.

                val uploadTask: StorageTask<*>
                uploadTask = fileref.putFile(imageUri!!)
                // Continuation -> Метод Firebase

                uploadTask.continueWithTask (Continuation <UploadTask.TaskSnapshot, Task<Uri>> { task ->
                    // Если !task.isSuccessful -> Не был выполнен. Throw error
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            throw it
                            progressDialog.dismiss() // Отключение progressDialog
                        }
                    }
                    // Если фотография загруженна успешна -> Загрузка нашей фотографии?
                    return@Continuation fileref.downloadUrl
                }).addOnCompleteListener (OnCompleteListener <Uri> { task ->
                    if (task.isSuccessful){
                        val downloadUrl = task.result // Реальное URL фотографии
                        myUrl = downloadUrl.toString()

                        val ref = FirebaseDatabase.getInstance().reference.child("Users")

                        val userMap = HashMap<String, Any>() //HashMap -> Загулил.
                        userMap["fullname"] = full_name_profile_frag.text.toString().toLowerCase()
                        userMap["username"] = username_profile_frag.text.toString().toLowerCase()
                        userMap["bio"] = bio_profile_frag.text.toString().toLowerCase()
                        userMap["image"] = myUrl // фотография

                        //сохранение + обновленных данных
                        ref.child(firebaseUser.uid).updateChildren(userMap)

                        Toast.makeText(this, "Информация была успешно обновлена!", Toast.LENGTH_LONG).show()

                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                        progressDialog.dismiss() // Отключение progressDialog
                    }
                    else {
                        progressDialog.dismiss() // Отключение progressDialog
                    }
                } )
            }
        }


    }





}
