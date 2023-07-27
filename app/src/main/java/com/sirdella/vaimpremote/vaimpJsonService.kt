package com.sirdella.vaimpremote

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.net.URL

class vaimpJsonService()
{
    var ready = false
    var moshi: Moshi
    var jsonAdapterLista: JsonAdapter<List<SongListDC>>
    var jsonAdapterEstado: JsonAdapter<PlaybackStateDC>

    init {
        Log.d("cosas", "inicio init vaimpJsonService")
        moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

        val typeLista = Types.newParameterizedType(List::class.java, SongListDC::class.java)

        jsonAdapterLista = moshi.adapter(typeLista)
        jsonAdapterEstado = moshi.adapter(PlaybackStateDC::class.java)

        ready = true
        Log.d("cosas", "fin init vaimpJsonService")
    }

    fun GetPlaylist(ip: String, callbackResultados: (ArrayList<SongListDC>) -> Unit)
    {
        Log.d("cosas", "GetPlaylist inicio")

        var json = ""
        try {
            json = URL("http://" + ip + "/GetPlaylist").readText()
        } catch (e: Exception) {
            Log.d("cosas", e.toString())
        }
        val canciones = jsonAdapterLista.fromJson(json)
        if (canciones != null)
        {
            for((index, i) in canciones.withIndex())
            {
                i.index = index
            }
            callbackResultados(canciones as ArrayList<SongListDC>)
        }
        Log.d("cosas", "GetPlaylist fin")
    }

    fun GetPlaybackState(ip: String, callbackResultado: (PlaybackStateDC) -> Unit)
    {
        var json = ""
        try {
            json = URL("http://" + ip + "/GetPlaybackState").readText()
        } catch (e: Exception) {
            Log.d("cosas", "ex: " + e.toString())
        }
        val cancion = jsonAdapterEstado.fromJson(json)
        if (cancion != null)
        {
            callbackResultado(cancion)
        }
    }
}