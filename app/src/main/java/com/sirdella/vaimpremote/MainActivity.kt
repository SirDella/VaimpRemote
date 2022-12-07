package com.sirdella.vaimpremote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class MainActivity : AppCompatActivity() {

    lateinit var receiver: broadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("cosas", "inicio mainActivity")

        val servicioVaimp = vaimpService()
        val app = (application as App)

        //Recycler:
        rvLista = findViewById<RecyclerView>(R.id.recyclerCanciones)
        adapterRecycler = CancionAdapter(this, callbackClick = {
            //ocultarTeclado()
            app.repoVaimp!!.servicioVaimp.SelectSong(app.repoVaimp!!.mainIp!!, it.index){

            }
        })
        rvLista.adapter = adapterRecycler

        //Broadcast de actualización:
        receiver = broadcastReceiver(callback = {
            rvLista.scheduleLayoutAnimation()
            adapterRecycler.actualizarLista((app.repoVaimp!!.listaCanciones))
            val ivLogo = findViewById<ImageView>(R.id.imageView)
            Glide.with(applicationContext).load(R.drawable.icon).transition(DrawableTransitionOptions.withCrossFade()).centerCrop().into(ivLogo)
        })

        IntentFilter("vaimpRefresh").also {
            registerReceiver(receiver, it)
        }

        if (app.repoVaimp != null && app.repoVaimp!!.listaCanciones != null)
        {
            adapterRecycler.actualizarLista(app.repoVaimp!!.listaCanciones)
        }

        val bPlay = findViewById<CardView>(R.id.imageViewPlay)
        bPlay.setOnClickListener {
            servicioVaimp.PlayPause(app.repoVaimp!!.mainIp!!){

            }
        }

        val bNext = findViewById<CardView>(R.id.imageViewNext)
        bNext.setOnClickListener {
            servicioVaimp.NextSong(app.repoVaimp!!.mainIp!!){

            }
        }

        val bPrev = findViewById<CardView>(R.id.imageViewPrev)
        bPrev.setOnClickListener {
            servicioVaimp.PreviousSong(app.repoVaimp!!.mainIp!!){

            }
        }

        Log.d("cosas", "fin mainActivity")
    }

    class broadcastReceiver(callback: (Boolean) -> Unit): BroadcastReceiver() {
        val callbak = callback
        override fun onReceive(context: Context?, intent: Intent?) {
            callbak(true)
        }
    }

    lateinit var adapterRecycler: CancionAdapter
    lateinit var rvLista: RecyclerView

    class CancionAdapter(private val contexto: Context, private val callbackClick: (SongListDC) -> Unit) : RecyclerView.Adapter<CancionVH>() {

        var canciones = listOf<SongListDC>()

        fun actualizarLista(nuevaLista: List<SongListDC>){
            canciones = nuevaLista
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CancionVH {
            val view = LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_cancion, parent, false)
            return CancionVH(view)
        }

        override fun onBindViewHolder(holder: CancionVH, position: Int) {
            val cancion = canciones[position]

            if(cancion.ImgUrl=="")
            {
                Glide.with(contexto).load(R.drawable.defaultsongimage).transition(DrawableTransitionOptions.withCrossFade()).centerCrop().into(holder.ivImagen)
            }
            else
            {
                Glide.with(contexto).load(cancion.ImgUrl).transition(DrawableTransitionOptions.withCrossFade()).centerCrop().into(holder.ivImagen)
            }
            holder.tvNombre.text = cancion.Name

            holder.itemView.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    callbackClick(cancion)
                }
            })
        }

        override fun getItemCount(): Int {
            return canciones.size
        }
    }

    class CancionVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre = itemView.findViewById<TextView>(R.id.textViewCancion)
        val ivImagen = itemView.findViewById<ImageView>(R.id.imageView)
        val cView = itemView.findViewById<CardView>(R.id.mainCard)
    }
}
