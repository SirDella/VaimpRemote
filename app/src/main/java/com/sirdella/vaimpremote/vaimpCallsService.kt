package com.sirdella.vaimpremote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class vaimpCallsService {

    fun isVAIMP(ip: String, callbackRespuesta: (Boolean) -> Unit){
        GlobalScope.launch (Dispatchers.IO){
            var respuesta = ""
            try {
                //var url = URL("http://" + ip + "/isVAIMP"
                respuesta = requestGET("http://" + ip + "/isVAIMP")
            } catch (e: Exception) {
                Log.d("cosas", ip + " " + e.toString())
                callbackRespuesta(false)
            }
            if (respuesta.contains("YES")) {
                Log.d("cosas", respuesta + ": " + ip)
                callbackRespuesta(true)
            }
        }
    }

    fun isVAIMPnoAsync(ip: String, callbackRespuesta: (Boolean) -> Unit, timeout: Int = 2000){
        var respuesta = ""
        try {
            //var url = URL("http://" + ip + "/isVAIMP"
            respuesta = requestGET("http://" + ip + "/isVAIMP", timeout)
        } catch (e: Exception) {
            Log.d("cosas", ip + " " + e.toString())
            callbackRespuesta(false)
        }
        if (respuesta.contains("YES")) {
            Log.d("cosas", respuesta + ": " + ip)
            callbackRespuesta(true)
        }
    }

    fun SelectSong(ip: String, index: Int, callbackRespuesta: (Boolean) -> Unit) {
        GlobalScope.launch (Dispatchers.IO){
            var respuesta = ""
            try {
                //var url = URL("http://" + ip + "/isVAIMP"
                respuesta = requestGET("http://" + ip + "/SelectSong=$index")
            } catch (e: Exception) {
                Log.d("cosas", ip + " " + e.toString())
                callbackRespuesta(false)
            }
            if (respuesta.contains("OK")) {
                Log.d("cosas", respuesta + ": " + ip)
                callbackRespuesta(true)
            }
        }
    }

    fun PlayPause(ip: String, callbackRespuesta: (Boolean) -> Unit) {
        GlobalScope.launch (Dispatchers.IO){
            var respuesta = ""
            try {
                //var url = URL("http://" + ip + "/isVAIMP"
                respuesta = requestGET("http://" + ip + "/PlayPause")
            } catch (e: Exception) {
                Log.d("cosas", ip + " " + e.toString())
                callbackRespuesta(false)
            }
            if (respuesta.contains("OK")) {
                Log.d("cosas", respuesta + ": " + ip)
                callbackRespuesta(true)
            }
        }
    }

    fun NextSong(ip: String, callbackRespuesta: (Boolean) -> Unit) {
        GlobalScope.launch (Dispatchers.IO){
            var respuesta = ""
            try {
                //var url = URL("http://" + ip + "/isVAIMP"
                respuesta = requestGET("http://" + ip + "/NextSong")
            } catch (e: Exception) {
                Log.d("cosas", ip + " " + e.toString())
                callbackRespuesta(false)
            }
            if (respuesta.contains("OK")) {
                Log.d("cosas", respuesta + ": " + ip)
                callbackRespuesta(true)
            }
        }
    }

    fun PreviousSong(ip: String, callbackRespuesta: (Boolean) -> Unit) {
        GlobalScope.launch (Dispatchers.IO){
            var respuesta = ""
            try {
                //var url = URL("http://" + ip + "/isVAIMP"
                respuesta = requestGET("http://" + ip + "/PreviousSong")
            } catch (e: Exception) {
                Log.d("cosas", ip + " " + e.toString())
                callbackRespuesta(false)
            }
            if (respuesta.contains("OK")) {
                Log.d("cosas", respuesta + ": " + ip)
                callbackRespuesta(true)
            }
        }
    }

    fun StopMusic(ip: String, callbackRespuesta: (Boolean) -> Unit)
    {
        GlobalScope.launch (Dispatchers.IO){
            var respuesta = ""
            try {
                //var url = URL("http://" + ip + "/isVAIMP"
                respuesta = requestGET("http://" + ip + "/StopMusic")
            } catch (e: Exception) {
                Log.d("cosas", ip + " " + e.toString())
                callbackRespuesta(false)
            }
            if (respuesta.contains("OK")) {
                Log.d("cosas", respuesta + ": " + ip)
                callbackRespuesta(true)
            }
        }
    }

    fun SetMusicPos(ip: String, sec: Float, callbackRespuesta: (Boolean) -> Unit)
    {
        GlobalScope.launch (Dispatchers.IO){
            var respuesta = ""
            try {
                //var url = URL("http://" + ip + "/isVAIMP"
                respuesta = requestGET("http://" + ip + "/SetMusicPos=$sec")
            } catch (e: Exception) {
                Log.d("cosas", ip + " " + e.toString())
                callbackRespuesta(false)
            }
            if (respuesta.contains("OK")) {
                Log.d("cosas", respuesta + ": " + ip)
                callbackRespuesta(true)
            }
        }
    }



    fun requestGET(url: String, timeout: Int = 2000): String {
        val obj = URL(url)
        val con = obj.openConnection() as HttpURLConnection
        con.requestMethod = "GET"
        con.connectTimeout=timeout
        con.readTimeout=timeout
        val responseCode = con.responseCode
        println("Response Code :: $responseCode")
        return if (responseCode == HttpURLConnection.HTTP_OK) { // connection ok
            val `in` =
                BufferedReader(InputStreamReader(con.inputStream))
            var inputLine: String?
            val response = StringBuffer()
            while (`in`.readLine().also { inputLine = it } != null) {
                response.append(inputLine)
            }
            `in`.close()
            response.toString()
        } else {
            ""
        }
    }
}
