package com.sirdella.vaimpremote

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File


class VaimpData(private val contexto: Context, val iniciarIpSel: Boolean = true) {
    var listaCanciones = ArrayList<SongListDC>()
    var cancionActual = PlaybackStateDC()

    var mainIp: String? = null
    var ips = ArrayList<IpListDC>()

    val logger = Logger(contexto)

    lateinit var servicioVaimpJson: VaimpJson
    var jsonServiceIniciado = false
    var servicioVaimp: VaimpCalls

    init {
        logger.log("Inicio vaimpData", "Flowlog")

        cargarConfig()
        servicioVaimp = VaimpCalls()
        actualizarIp()

        logger.log("Fin vaimpData", "Flowlog")
    }

    fun iniciarJsonService(){
        GlobalScope.launch {
            if (!jsonServiceIniciado) servicioVaimpJson = VaimpJson()
            jsonServiceIniciado = true }
    }

    fun actualizarIp() {
        validarIp(mainIp) {
            if (it) {
                guardarConfig()

                if (!jsonServiceIniciado) servicioVaimpJson = VaimpJson()

                jsonServiceIniciado = true
                servicioVaimpJson.GetPlaylist(mainIp!!) { lista ->
                    listaCanciones = lista
                }
                servicioVaimpJson.GetPlaybackState(mainIp!!) { cancion ->
                    cancionActual = cancion
                }

                val intent = Intent("vaimpRefresh")
                contexto.sendBroadcast(intent)
            } else if (iniciarIpSel) {
                logger.log("Lanzando IpSelectionActivity", "Flowlog")
                val intent = Intent(contexto, IpSelectionActivity::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                contexto.startActivity(intent)
            }
        }
    }

    fun agregarIp(ipstr: String)
    {
        for(ip in ips)
        {
            if (ip.address == ipstr)
            {
                return
            }
        }

        val ip = IpListDC(ipstr, true)
        ips.add(ip)
        guardarConfig()
    }

    fun guardarConfig(){
        logger.log("Guardando datos", "Flowlog")
        val path = contexto.getExternalFilesDir(null)
        val file = File(path, "Data.txt")
        file.delete()

        file.appendText("Reference:\n" +
                "- ip,<address_str>,<online_bool>\n" +
                "- mainIp,<address>\n\n")

        for(i in ips)
        {
            file.appendText("ip,"+i.address + "," + i.online.toString() +"\n")
        }

        file.appendText("mainIp," + mainIp + "\n")
    }

    fun cargarConfig()
    {
        logger.log("Cargando datos", "Flowlog")

        val path = contexto.getExternalFilesDir(null)
        val file = File(path, "Data.txt")

        if (file.exists())
        {
            val filelines = file.readLines()
            for(line in filelines)
            {
                val linesplit = line.split(",")
                when(linesplit[0]){
                    "mainIp" -> {
                        mainIp = linesplit[1]
                    }

                    "ip" -> {
                        ips.add(IpListDC(linesplit[1], linesplit[2].toBoolean()))
                    }
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