package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    val authManager = AuthManager(application)
}
