package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.data.PayMitraDatabase
import com.example.data.PayMitraRepository
import com.example.ui.PayMitraViewModel
import com.example.ui.screens.PayMitraNavigation
import com.example.ui.theme.MyApplicationTheme

class PayMitraViewModelFactory(private val repository: PayMitraRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PayMitraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PayMitraViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MainActivity : ComponentActivity() {
    private val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            PayMitraDatabase::class.java,
            "paymitra_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    private val repository by lazy {
        PayMitraRepository(database.payMitraDao())
    }

    private val viewModel by lazy {
        ViewModelProvider(this, PayMitraViewModelFactory(repository))[PayMitraViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PayMitraNavigation(viewModel = viewModel)
            }
        }
    }
}

