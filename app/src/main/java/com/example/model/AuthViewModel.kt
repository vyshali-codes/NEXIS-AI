package com.example.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val authManager = FirebaseAuthenticationManager()

    val authState: StateFlow<AuthState> = authManager.authState
    val error: StateFlow<String?> = authManager.error

    override fun onCleared() {
        super.onCleared()
        authManager.clear()
    }

    fun signUp(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            return
        }
        viewModelScope.launch {
            authManager.signUp(email, pass)
        }
    }

    fun signIn(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            return
        }
        viewModelScope.launch {
            authManager.signIn(email, pass)
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            authManager.signInWithGoogle(idToken)
        }
    }

    fun signOut() {
        authManager.signOut()
    }

    fun setError(message: String) {
        authManager.setError(message)
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val email: String) : AuthState()
}
