package com.sirdella.vaimpremote

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File


class vaimpRepo(private val contexto: Context) {
    var listaCanciones = ArrayList<SongListDC>()
    var cancionActual = PlaybackStateDC()

    var mainIp: String? = null
    var ips = ArrayList<IpListDC>()

    lateinit var servicioVaimpJson: vaimpJsonService
    var jsonServiceIniciado = false
    var servicioVaimp: vaimpCallsService

    init {
        Log.d("Cosas", "inicio init vaimpRepo")

        cargarConfig()
        servicioVaimp = vaimpCallsService()
        actualizarIp()

        Log.d("Cosas", "fin init vaimpRepo")
    }

    fun iniciarJsonService(){
        GlobalScope.launch {
            if (!jsonServiceIniciado) servicioVaimpJson = vaimpJsonService()
            jsonServiceIniciado = true }
    }

    fun actualizarIp() {
        validarIp(mainIp) {
            if (it) {
                guardarConfig()

                if (!jsonServiceIniciado) servicioVaimpJson = vaimpJsonService()

                jsonServiceIniciado = true
                servicioVaimpJson.GetPlaylist(mainIp!!) { lista ->
                    listaCanciones = lista
                }
                servicioVaimpJson.GetPlaybackState(mainIp!!) { cancion ->
                    cancionActual = cancion
                }

                val intent = Intent("vaimpRefresh")
                contexto.sendBroadcast(intent)
            } else {
                val intent = Intent(contexto, IpSelectionActivity::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                contexto.startActivity(intent)
            }
        }
    }

    private fun guardarConfig(){
        val path = contexto.getExternalFilesDir(null)
        val file = File(path, "Data.txt")
        file.delete()

        for(i in ips)
        {
            file.appendText("ip="+i.adress + "\n")
        }

        file.appendText("mainIp=" + mainIp + "\n")
    }

    private fun cargarConfig()
    {
        val path = contexto.getExternalFilesDir(null)
        val file = File(path, "Data.txt")

        if (file.exists())
        {
            val filelines = file.readLines()
            for(line in filelines)
            {
                val linesplit = line.split("=")
                when(linesplit[0]){
                    "mainIp" -> mainIp = linesplit[1]
                    "ip" -> ips.add(IpListDC(linesplit[1]))
                }
            }
        }
    }
    fun validarIp(ip: String?, callbackValida: (Boolean) -> Unit)
    {
        if (mainIp == null) {
            callbackValida(false)
        }
        else
        {
            servicioVaimp.isVAIMP(mainIp.toString()){
                if (it)
                {
                    callbackValida(true)
                }
                else
                {
                    callbackValida(false)
                }
            }
        }
    }
}