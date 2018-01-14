package com.overswell.firebasetoy

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.arch.persistence.room.*
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_main.*
import java.math.BigDecimal

class MainActivity : AppCompatActivity() {

    val viewModel by lazy {
        ViewModelProviders.of(this).get(MainViewModel::class.java)
    }

    private val RC_SIGNUP = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val apiClient = GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API,
                        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .build())
                .enableAutoManage(this) {
                    viewModel.failed()
                }
                .build()

        button_signin.setOnClickListener {
            startActivityForResult(
                    Auth.GoogleSignInApi.getSignInIntent(apiClient)
                    , RC_SIGNUP)
        }
        button_signout.setOnClickListener {
            Auth.GoogleSignInApi.signOut(apiClient).setResultCallback {
                textView_output.text = "SIGN OUT"
            }
        }

        viewModel.textOutput.observe(this, Observer { textView_output.text = it })
        viewModel.user.observe(this, Observer {
            textView_output.text = "${it?.id} ${it?.displayName}"
        })


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RC_SIGNUP -> handleSigninResult(Auth.GoogleSignInApi.getSignInResultFromIntent(data))
        }
    }

    private fun handleSigninResult(result: GoogleSignInResult?) {
        if (result != null && result.isSuccess) {
            viewModel.setUsername(result.signInAccount)
        }

    }

    override fun onStart() {
        super.onStart()
        val currentUser = viewModel.auth.currentUser
        updateUI(currentUser)
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        Toast
                .makeText(this, currentUser?.displayName, Toast.LENGTH_LONG)
                .show()
    }

}

class MainViewModel : ViewModel() {
    // Write a message to the database
    val textOutput by lazy { MutableLiveData<String>() }
    val user by lazy { MutableLiveData<GoogleSignInAccount>() }
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val database by lazy { FirebaseDatabase.getInstance() }
    val ref = database.getReference("message")
    fun s() = ref.setValue(Account(name = "Hello, World!"))

    fun failed() {
        textOutput.value = "Connection failure"
    }

    fun setUsername(signInAccount: GoogleSignInAccount?) {
        user.value = signInAccount
    }

}

data class Account(val name: String)
data class Transaction(val amount: BigDecimal, val credit: Account, val debit: Account)

@Entity
data class User(
        @PrimaryKey(autoGenerate = true) val uid: Int,
        @ColumnInfo(name = "first_name") val firstName: String,
        @ColumnInfo(name = "last_name") val lastName: String)

@Dao
interface UserDao {
    @get:Query("SELECT * FROM user order by last_name")
    val all: List<User>

    @Query("SELECT * FROM user WHERE uid IN (:userIds)")
    fun loadAllByIds(userIds: IntArray): List<User>

    @Query("SELECT * FROM user WHERE first_name LIKE :first AND " + "last_name LIKE :last LIMIT 1")
    fun findByName(first: String, last: String): User

    @Insert
    fun insertAll(vararg users: User)

    @Delete
    fun delete(user: User)
}

@Database(entities = [User::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): UserDao
}

@Database(entities = [PersistentClicks::class], version = 1, exportSchema = false)
abstract class ClickPersistence : RoomDatabase() {
    abstract fun clickDao(): ClickDao
}

interface ClickDao {
    @get:Query("select * from persistentClicks limit 1")
    val entry: List<PersistentClicks>

}

data class PersistentClicks(var clicks: Int)
