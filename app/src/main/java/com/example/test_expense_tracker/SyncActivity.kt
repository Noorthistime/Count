package com.example.test_expense_tracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.test_expense_tracker.data.ExpenseDatabase
import com.example.test_expense_tracker.databinding.ActivitySyncBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class SyncActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySyncBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var syncManager: FirebaseSyncManager

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySyncBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        val dao = ExpenseDatabase.getDatabase(this).expenseDao()
        syncManager = FirebaseSyncManager(dao)

        setupUI()
        updateUI(auth.currentUser != null)
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnGoogleSignin.setOnClickListener {
            try {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val signInClient = GoogleSignIn.getClient(this, gso)
                signInLauncher.launch(signInClient.signInIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Sign-in initialization failed", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSyncNow.setOnClickListener {
            startSync()
        }

        binding.btnSignout.setOnClickListener {
            auth.signOut()
            GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
            updateUI(false)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    updateUI(true)
                    Toast.makeText(this, "Signed in successfully", Toast.LENGTH_SHORT).show()
                } else {
                    val error = task.exception?.message ?: "Unknown error"
                    Toast.makeText(this, "Firebase Auth Failed: $error", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun updateUI(isSignedIn: Boolean) {
        if (isSignedIn) {
            binding.tvStatus.text = "Signed in as ${auth.currentUser?.email}"
            binding.btnGoogleSignin.visibility = View.GONE
            binding.btnSyncNow.visibility = View.VISIBLE
            binding.btnSignout.visibility = View.VISIBLE
        } else {
            binding.tvStatus.text = "Not Signed In"
            binding.btnGoogleSignin.visibility = View.VISIBLE
            binding.btnSyncNow.visibility = View.GONE
            binding.btnSignout.visibility = View.GONE
        }
    }

    private fun startSync() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSyncNow.isEnabled = false
        
        lifecycleScope.launch {
            try {
                syncManager.syncData()
                Toast.makeText(this@SyncActivity, "Sync Completed", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@SyncActivity, "Sync Failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnSyncNow.isEnabled = true
            }
        }
    }
}