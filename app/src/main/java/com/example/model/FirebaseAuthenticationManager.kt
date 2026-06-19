package com.example.model

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthenticationManager {

    private var auth: FirebaseAuth? = null
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    init {
        try {
            auth = FirebaseAuth.getInstance()
            setupAuthStateListener()
        } catch (e: Exception) {
            auth = null
            _error.value = "Authentication not available at the moment."
            _authState.value = AuthState.Unauthenticated
        }
    }

    private fun setupAuthStateListener() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                _authState.value = AuthState.Authenticated(user.email ?: "Unknown Email")
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
        auth?.addAuthStateListener(authStateListener!!)
    }

    suspend fun signUp(email: String, pass: String): Boolean {
        if (auth == null) {
            _error.value = "Authentication not available."
            return false
        }
        _error.value = null
        _authState.value = AuthState.Loading
        
        return try {
            val result = auth!!.createUserWithEmailAndPassword(email, pass).await()
            _authState.value = AuthState.Authenticated(result.user?.email ?: email)
            true
        } catch (e: com.google.firebase.auth.FirebaseAuthWeakPasswordException) {
            _error.value = "Weak password: ${e.reason}"
            _authState.value = AuthState.Unauthenticated
            false
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            _error.value = "Invalid email format."
            _authState.value = AuthState.Unauthenticated
            false
        } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            _error.value = "Email is already registered."
            _authState.value = AuthState.Unauthenticated
            false
        } catch (e: Exception) {
            _error.value = "An error occurred during sign up."
            _authState.value = AuthState.Unauthenticated
            false
        }
    }

    suspend fun signIn(email: String, pass: String): Boolean {
        if (auth == null) {
            _error.value = "Authentication not available."
            return false
        }
        _error.value = null
        _authState.value = AuthState.Loading
        
        return try {
            val result = auth!!.signInWithEmailAndPassword(email, pass).await()
            _authState.value = AuthState.Authenticated(result.user?.email ?: email)
            true
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            _error.value = "User not found. Please register."
            _authState.value = AuthState.Unauthenticated
            false
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            _error.value = "Invalid email or password."
            _authState.value = AuthState.Unauthenticated
            false
        } catch (e: Exception) {
            _error.value = "An error occurred during sign in."
            _authState.value = AuthState.Unauthenticated
            false
        }
    }

    suspend fun signInWithGoogle(idToken: String): Boolean {
        if (auth == null) {
            _error.value = "Authentication not available."
            return false
        }
        _error.value = null
        _authState.value = AuthState.Loading

        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val result = auth!!.signInWithCredential(credential).await()
            _authState.value = AuthState.Authenticated(result.user?.email ?: "Unknown Email")
            true
        } catch (e: Exception) {
            _error.value = "Google Sign In failed. Please try again."
            _authState.value = AuthState.Unauthenticated
            false
        }
    }

    fun signOut() {
        auth?.signOut()
        _authState.value = AuthState.Unauthenticated
    }

    fun setError(message: String) {
        _error.value = message
        _authState.value = AuthState.Unauthenticated
    }

    fun clear() {
        authStateListener?.let { auth?.removeAuthStateListener(it) }
    }
}
