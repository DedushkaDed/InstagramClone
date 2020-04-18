package glebkalinin.test.instagramclone

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_sign_up.*

class SignUpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        signin_link_btn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }

        signup_btn.setOnClickListener {
            CreateAccount()
        }
    }

    private fun CreateAccount() {
        val fullName = fullname_signup.text.toString()
        val userName = username_signup.text.toString()
        val email = email_signup.text.toString()
        val password = passwrod_signup.text.toString()

        when{
            TextUtils.isEmpty(fullName) -> Toast.makeText(this, "Вы забыли ввести имя.", Toast.LENGTH_LONG).show()
            TextUtils.isEmpty(userName) -> Toast.makeText(this, "Вы забыли ввести никнейм.", Toast.LENGTH_LONG).show()
            TextUtils.isEmpty(email) -> Toast.makeText(this, "Вы забыли ввести почту.", Toast.LENGTH_LONG).show()
            TextUtils.isEmpty(password) -> Toast.makeText(this, "Вы забыли ввести пароль.", Toast.LENGTH_LONG).show()

            else -> {
                val progressDialog = ProgressDialog(this@SignUpActivity) // ProgressDialog
                progressDialog.setTitle("Регистрация нового пользователя") //Title
                progressDialog.setMessage("Пожалуйста подождите, это займёт несколько секунд...") // Вывод ProgessDialog -> пользователю
                progressDialog.setCanceledOnTouchOutside(false) // Запрещаем пользователю закрыть
                progressDialog.show()

                val mAuth: FirebaseAuth = FirebaseAuth.getInstance()

                mAuth.createUserWithEmailAndPassword(email,password)
                    .addOnCompleteListener{task ->
                        if (task.isSuccessful){
                            saveUserInfo(fullName, userName, email, progressDialog)
                            progressDialog.dismiss()
                        }
                        else {
                            val message = task.exception!!.toString()
                            Toast.makeText(this, "Ошибка$message", Toast.LENGTH_LONG).show()
                            mAuth.signOut() // Если пользователь не авторизован?
                            progressDialog.dismiss() // Завершение progressDialog.

                        }
                    }
            }
        }
    }

    private fun saveUserInfo(fullName: String, userName: String, email: String, progressDialog: ProgressDialog) {
        val currentUserID = FirebaseAuth.getInstance().currentUser!!.uid // Уникальный ID текущего пользователя.
        val usersRef: DatabaseReference = FirebaseDatabase.getInstance().reference.child("Users") // Вся информация о пользователях, хранящаяся в Firebase DataBase.

        val userMap = HashMap<String, Any>() //HashMap -> Загулил.
        userMap["uid"] = currentUserID
        userMap["fullname"] = fullName.toLowerCase()
        userMap["username"] = userName.toLowerCase()
        userMap["email"] = email
        userMap["bio"] = "Привет! Я использую приложение Глеба Калинина!"
        userMap["image"] = "https://firebasestorage.googleapis.com/v0/b/instagramclone-7ba18.appspot.com/o/Default%20Images%2Fprofile.png?alt=media&token=9e83e426-d799-4246-8e59-769cb8b45fa0" // Дефолтная фотография для всех пользователей. Находится в FireBase storage.

        usersRef.child(currentUserID).setValue(userMap)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Учётная запись успешно создана!", Toast.LENGTH_LONG).show()

                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                }
                else {
                    val message = task.exception!!.toString()
                    Toast.makeText(this, "Ошибка$message", Toast.LENGTH_LONG).show()
                    FirebaseAuth.getInstance().signOut()
                    progressDialog.dismiss()
                }
            }

    }
}
