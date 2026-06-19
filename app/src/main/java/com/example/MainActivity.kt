package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AuthState
import com.example.model.AuthViewModel
import com.example.ui.AuthScreen
import com.example.ui.ChatScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDarkTheme) }

            val authViewModel: AuthViewModel = viewModel()
            val authState by authViewModel.authState.collectAsState()
            val chatViewModel: com.example.model.ChatViewModel = viewModel()
            val remoteIsDarkTheme by chatViewModel.isDarkTheme.collectAsState()
            val themeColor by chatViewModel.themeColor.collectAsState()
            
            androidx.compose.runtime.LaunchedEffect(authState) {
                when (val state = authState) {
                    is AuthState.Authenticated -> {
                        chatViewModel.setUser(state.email)
                    }
                    is AuthState.Unauthenticated -> {
                        chatViewModel.setUser(null)
                    }
                    else -> {}
                }
            }
            
            var showAuthScreen by remember { mutableStateOf(false) }
            val currentTheme = remoteIsDarkTheme ?: isDarkTheme

            MyApplicationTheme(darkTheme = currentTheme, themeColor = themeColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showAuthScreen && authState !is AuthState.Authenticated) {
                        AuthScreen(
                            authViewModel = authViewModel, 
                            onAuthenticated = { showAuthScreen = false },
                            onDismiss = { showAuthScreen = false }
                        )
                    } else {
                        ChatScreen(
                            viewModel = chatViewModel,
                            onRequireAuth = { showAuthScreen = true },
                            authState = authState,
                            onSignOut = { authViewModel.signOut() },
                            isDarkTheme = currentTheme,
                            onToggleTheme = {
                                isDarkTheme = !currentTheme
                                chatViewModel.setTheme(!currentTheme)
                            },
                            currentThemeColor = themeColor,
                            onThemeColorSelected = { chatViewModel.setThemeColor(it) }
                        )
                    }
                }
            }
        }
    }
}
