package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.firebase.configureAppCheck
import com.example.ui.MainWorkspaceShell
import com.example.ui.screens.LandingAuthScreen
import com.example.ui.theme.DataPilotTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            DataPilotTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val auth = remember { Firebase.auth }
    var currentUser by remember { mutableStateOf<FirebaseUser?>(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        LandingAuthScreen(
            onAuthSuccess = {
                currentUser = auth.currentUser
            }
        )
    } else {
        key(user.uid) {
            MainWorkspaceShell(
                currentUserId = user.uid,
                displayName = user.displayName.orEmpty(),
                email = user.email.orEmpty(),
                onSignOutComplete = {
                    currentUser = auth.currentUser
                },
            )
        }
    }
}
