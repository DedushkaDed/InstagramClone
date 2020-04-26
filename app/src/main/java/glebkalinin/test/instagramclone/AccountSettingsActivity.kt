package glebkalinin.test.instagramclone

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import glebkalinin.test.instagramclone.Model.User
import kotlinx.android.synthetic.main.activity_account_settings.*
import kotlinx.android.synthetic.main.fragment_profile.view.*

class AccountSettingsActivity : AppCompatActivity() {

    private lateinit var firebaseUser: FirebaseUser
    private var checker = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_settings)


        firebaseUser = FirebaseAuth.getInstance().currentUser!!


        logout_btn.setOnClickListener {
            FirebaseAuth.getInstance().signOut() //Выход из аккаунта
            val intent = Intent(this, SignInActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        save_infor_profile_btn.setOnClickListener {
            if (checker == "clicked")
            {

            }
            else
            {
                updateUserInfoOnly()
            }
        }


        userInfo()
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
                Toast.makeText(this, "Пожалуйста,введите описание своего профиля", Toast.LENGTH_LONG).show()
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


    private fun userInfo(){
        val userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(firebaseUser.uid)

        userRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {

                if (p0.exists())
                {
                    val user = p0.getValue<User>(User::class.java)

                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.profile).into(profile_image_view_profile_frag) // Photo пользователя
                    username_profile_frag.setText(user!!.getUsername()) // username
                    full_name_profile_frag.setText(user!!.getFullname()) // fullname
                    bio_profile_frag.setText(user!!.getBio()) // bio

                }
            }

            override fun onCancelled(p0: DatabaseError) {

            }
        })
    }
}
