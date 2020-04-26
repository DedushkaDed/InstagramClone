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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.android.synthetic.main.activity_account_settings.*
import kotlinx.android.synthetic.main.activity_add_post.*

class AddPostActivity : AppCompatActivity() {

    private var myUrl = ""
    private var imageUri: Uri? = null
    private var storagePostPictureReference: StorageReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_post)

        storagePostPictureReference = FirebaseStorage.getInstance().reference.child("Posts Pictures")

        save_new_post_btn.setOnClickListener { uploadImage() }


        CropImage.activity()
            .setAspectRatio(2, 1) // разрешение картинки
            .start(this)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val result = CropImage.getActivityResult(data) // Результат
            imageUri = result.uri
            image_post.setImageURI(imageUri) // Отображение фотки
        }
        else {
            Toast.makeText(this, "Что-то не то с твоей фотографией... Давай-ка другую!", Toast.LENGTH_LONG).show()
        }
    }


    private fun uploadImage() {
        when {
            imageUri == null -> Toast.makeText(this, "Пожалуйста,выберите фотографию", Toast.LENGTH_LONG).show()

            description_post.text.toString() == "" -> {
                Toast.makeText(this, "Вы забыли добавить описание к фотографии", Toast.LENGTH_LONG).show()
            }

            else -> {

                val progressDialog = ProgressDialog(this)
                progressDialog.setTitle("Добавляем новую публикацию")
                progressDialog.setMessage("Пожалуйста подождите,мы загружаем вашу публикацию...")
                progressDialog.show()

                val fileref = storagePostPictureReference!!.child(System.currentTimeMillis().toString() + ".jpg") //  Обновление старой фотографии на новую. У каждой фотографии уникальный ID -> System.currentTimeMillis().toString()

                val uploadTask: StorageTask<*>
                uploadTask = fileref.putFile(imageUri!!)            // ->          !! - not null

                uploadTask.continueWithTask (Continuation <UploadTask.TaskSnapshot, Task<Uri>> { task ->
                    // Если !task.isSuccessful -> Не был выполнен. Throw error
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            throw it
                            progressDialog.dismiss() // Отключение progressDialog
                        }
                    }

                    return@Continuation fileref.downloadUrl
                })
                    .addOnCompleteListener (OnCompleteListener <Uri> { task ->
                    if (task.isSuccessful){
                        val downloadUrl = task.result
                        myUrl = downloadUrl.toString()

                        val ref = FirebaseDatabase.getInstance().reference.child("Posts")
                        val postId = ref.push().key // Рандомный ID -> для каждого загруженного поста.

                        val postMap = HashMap<String, Any>()
                        postMap["postid"] = postId!!
                        postMap["description"] = description_post.text.toString().toLowerCase()
                        postMap["publisher"] = FirebaseAuth.getInstance().currentUser!!.uid
                        postMap["postimage"] = myUrl

                        //сохранение + обновленных данных
                        ref.child(postId).updateChildren(postMap)

                        Toast.makeText(this, "Публикация успешно загружена", Toast.LENGTH_LONG).show()

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
