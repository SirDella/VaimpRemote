package com.sirdella.vaimpremote

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.*
import kotlin.collections.ArrayList


class IpSelectionActivity : AppCompatActivity() {
    lateinit var localIp: String
    var listaIps = ArrayList<ipListDC>()
    var cantIpsCheckeadas = 0

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ip_selection)
        Log.d("cosas", "inicio ipSelAct")

        val servicioVaimpIp = vaimpService()
        val app = (application as App)

        //Recycler:
        rvLista = findViewById<RecyclerView>(R.id.recyclerCanciones)
        adapterRecycler = ipAdapter(this, callbackClick = {ip->
            //ocultarTeclado()
            servicioVaimpIp.isVAIMP(ip.adress){
                if (it)
                {
                    app.repoVaimp!!.mainIp = ip.adress
                    app.repoVaimp!!.actualizarIp()
                    super.finish()
                }
            }
        })
        rvLista.adapter = adapterRecycler

        //SwipeRefreshLayout:
        val refreshLayout = findViewById<SwipeRefreshLayout>(R.id.refreshLayout)
        refreshLayout.setOnRefreshListener {
            busquedaIps(servicioVaimpIp, refreshLayout, app)
        }
        refreshLayout.setColorSchemeColors(this.getColor(R.color.vaimpGreen))
        refreshLayout.setProgressBackgroundColorSchemeColor(this.getColor(R.color.fondoOscuro2))

        busquedaIps(servicioVaimpIp, refreshLayout, app)
        app.repoVaimp!!.iniciarJsonService()

        var timer = Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (app.repoVaimp != null && app.repoVaimp!!.mainIp != null)
                {
                    servicioVaimpIp.isVAIMP(app.repoVaimp!!.mainIp!!){
                        if (it)
                        {
                            app.repoVaimp!!.actualizarIp()
                            this@IpSelectionActivity.finish()
                            this.cancel()
                        }
                    }
                }
            }
        }, 0, 1000)
    }

    private fun busquedaIps(servicioVaimpIp: vaimpService, refresh: SwipeRefreshLayout, app: App){
        listaIps = ArrayList()
        adapterRecycler.actualizarLista(listaIps)
        for(i in app.repoVaimp!!.ips)
        {
            listaIps.add(i)
        }

        for(i in listaIps)
        {
            servicioVaimpIp.isVAIMP(i.adress) {
                i.online = it
                runOnUiThread { adapterRecycler.actualizarLista(listaIps) }
            }
        }

        refresh.isRefreshing = true
        localIp = getIPHostAddress()
        cantIpsCheckeadas = 0
        val ipSplit = localIp.split(".")
        var ipWithoutLast = ""
        var i = 0
        while (i < ipSplit.size - 1) {
            ipWithoutLast += ipSplit[i] + "."
            i++
        }

        for (i in 0..255)
        {
            llamarIp(servicioVaimpIp, ipWithoutLast, i, refresh)
        }
    }

    private fun llamarIp(
        servicioVaimpIp: vaimpService,
        ipWithoutLast: String,
        i: Int,
        refresh: SwipeRefreshLayout
    ) {
        GlobalScope.launch(Dispatchers.IO) {
            Log.d("ips", i.toString())
            servicioVaimpIp.isVAIMPnoAsync(ipWithoutLast + i + ":5045", {
                if (it) {
                    val ip = ipListDC(ipWithoutLast + i + ":5045", true)
                    listaIps.add(ip)
                    runOnUiThread { adapterRecycler.actualizarLista(listaIps) }
                }
            }, 1000)
            if (i == 255) {
                runOnUiThread { refresh.isRefreshing = false }
            }
        }
    }

    fun getIPHostAddress(): String {
        NetworkInterface.getNetworkInterfaces()?.toList()?.map { networkInterface ->
            networkInterface.inetAddresses?.toList()?.find {
                !it.isLoopbackAddress && it is Inet4Address
            }?.let { return it.hostAddress }
        }
        return ""
    }

    lateinit var adapterRecycler: ipAdapter
    lateinit var rvLista: RecyclerView

    class ipAdapter(private val contexto: Context, private val callbackClick: (ipListDC) -> Unit) : RecyclerView.Adapter<ipVH>() {

        var ips = listOf<ipListDC>()

        fun actualizarLista(nuevaLista: List<ipListDC>){
            ips = nuevaLista
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ipVH {
            val view = LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_ip, parent, false)
            return ipVH(view)
        }

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onBindViewHolder(holder: ipVH, position: Int) {
            val ip = ips[position]

            holder.tvNombre.text = ip.adress
            if (ip.online)
            {
                holder.cView.setCardBackgroundColor(contexto.getColor(R.color.ipGreen))
            }
            else
            {
                holder.cView.setCardBackgroundColor(contexto.getColor(R.color.ipGray))
            }

            holder.itemView.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    callbackClick(ip)
                }
            })
        }

        override fun getItemCount(): Int {
            return ips.size
        }
    }

    class ipVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre = itemView.findViewById<TextView>(R.id.textViewCancion)
        val cView = itemView.findViewById<CardView>(R.id.cardViewLista)
    }
}