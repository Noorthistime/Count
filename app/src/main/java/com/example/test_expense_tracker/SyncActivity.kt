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
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class SyncActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySyncBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var syncManager: FirebaseSyncManager
    private lateinit var signInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val idToken = account.idToken
                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken)
                } else {
                    Toast.makeText(this, "Google token is null. Verify Firebase configuration.", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                val errorMsg = when (e.statusCode) {
                    10 -> "Developer Error: Check SHA-1 and Client ID in Firebase"
                    7 -> "Network Error: Check your connection"
                    12500 -> "Sign-In Failed: Code 12500 (usually SHA-1 mismatch)"
                    else -> "Google Error: ${e.statusCode}"
                }
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            }
        } else if (result.resultCode != RESULT_CANCELED) {
            Toast.makeText(this, "Sign-in interrupted (Code: ${result.resultCode})", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val theme = ThemeStorage.getTheme(this)
        setTheme(ThemeStorage.getThemeResource(theme))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySyncBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                kotlin.math.max(systemBars.bottom, ime.bottom)
            )
            insets
        }

        auth = FirebaseAuth.getInstance()
        val dao = ExpenseDatabase.getDatabase(this).expenseDao()
        syncManager = FirebaseSyncManager(dao)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        signInClient = GoogleSignIn.getClient(this, gso)

        setupUI()
        updateUI(auth.currentUser != null)
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnGoogleSignin.setOnClickListener {
            signInLauncher.launch(signInClient.signInIntent)
        }

        binding.btnSyncNow.setOnClickListener {
            startSync()
        }

        binding.btnSignout.setOnClickListener {
            auth.signOut()
            signInClient.signOut().addOnCompleteListener {
                updateUI(false)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    updateUI(true)
                    showSuccessDashboard()
                } else {
                    val error = task.exception?.message ?: "Unknown error"
                    Toast.makeText(this, "Firebase Auth Failed: $error", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun showSuccessDashboard() {
        val user = auth.currentUser
        val name = user?.displayName ?: "User"
        val email = user?.email ?: ""
        
        val message = "Sign-in Successful\nAccount: $email"
        
        Snackbar.make(binding.root, message, Snackbar.LENGTH_INDEFINITE)
            .setAction("DISMISS") { }
            .setActionTextColor(ThemeStorage.getColorPrimary(this))
            .setBackgroundTint(getColor(R.color.nothing_grey_dark))
            .setTextColor(getColor(R.color.white))
            .setAnchorView(binding.btnSyncNow)
            .show()
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