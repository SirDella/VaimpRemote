package com.sirdella.vaimpremote

import android.app.Activity
import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class App: Application() {
    var repoVaimp: vaimpRepo? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("cosas", "inicio app")

        GlobalScope.launch(Dispatchers.IO) {
            repoVaimp = vaimpRepo(applicationContext)
        }
        Log.d("cosas", "fin app")
    }
}
