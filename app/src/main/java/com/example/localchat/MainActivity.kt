package com.example.localchat

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.localchat.view.EmailScreen
import com.example.localchat.view.LoginScreen
import com.example.localchat.view.MainScreen
import com.example.localchat.view.MessageScreen
import com.example.localchat.view.PasswordScreen
import com.example.localchat.view.ProfileScreen
import com.example.mychat.viewmodel.LoginViewModel
import kotlin.getValue

class MainActivity : ComponentActivity() {
    val viewModel : LoginViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                    0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        }

        setContent {
            val isLoading by viewModel.isLoading.collectAsState()
            val controller = rememberNavController()
            Box(Modifier.fillMaxSize()) {
                NavHost(controller, startDestination = if(viewModel.firebaseAuth.currentUser?.uid !=null)"chat" else "email") {
                   composable("email"){
                       EmailScreen(viewModel,controller)
                   }
                    composable("password") {
                        PasswordScreen(viewModel,controller)
                    }
                    composable("login") {
                        LoginScreen(viewModel,controller)
                    }
                    composable("profile") {
                        ProfileScreen(viewModel,controller)
                    }
                    composable("chat") {
                        MainScreen(viewModel,controller)
                    }
                    composable("message") {
                        MessageScreen(viewModel, controller)
                    }

                }
            }
            if(isLoading) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(0.3f)), contentAlignment = Alignment.Center){
                    CircularProgressIndicator()
                }
            }
        }
    }
    override fun onStart() {
        super.onStart()
        val uuid = viewModel.firebaseAuth.currentUser?.uid ?: return
        val update = hashMapOf<String, Any>(
            "isOnline" to true,
            "lastSeen" to 0L

        )
        viewModel.firebaseFireStore.collection("users").document(uuid)
            .update(update)
    }

    override fun onStop() {
        super.onStop()
        val uuid = viewModel.firebaseAuth.currentUser?.uid ?: return
        val update = hashMapOf<String, Any>(
            "isOnline" to false,
            "lastSeen" to System.currentTimeMillis()
        )
        viewModel.firebaseFireStore.collection("users").document(uuid)
            .update(update)
    }
}
