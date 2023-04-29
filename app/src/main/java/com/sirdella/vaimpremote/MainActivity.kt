package com.sirdella.vaimpremote

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL

class MainActivity : AppCompatActivity() {

    lateinit var receiver: broadcastReceiver
    lateinit var mediaPlayer: MediaPlayer

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

        /*
        GlobalScope.launch{
            val file = File(applicationContext.getExternalFilesDir(null), "app.apk")
            if (file.exists()) file.delete()
            file.appendBytes(URL("https://github.com/SirDella/VaimpRemote/raw/master/app/build/outputs/apk/debug/app-debug.apk").readBytes())

            val intent = Intent(Intent.ACTION_VIEW)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val uri = FileProvider.getUriForFile(applicationContext, applicationContext.packageName + ".provider", file)
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            applicationContext.startActivity(intent);
        }
         */
        /*
        GlobalScope.launch{
            val file = File(applicationContext.getExternalFilesDir(null), "song.m4a")
            if (file.exists()) file.delete()
            file.appendBytes(URL("http://192.168.1.37:5045/dou").readBytes())

        }

         */


        mediaPlayer = MediaPlayer()
        GlobalScope.launch {
            var audioUrl = "http://sirdella.ddns.net:5045/dou"

            // on below line we are setting audio stream
            // type as stream music on below line.
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)

            // on below line we are running a try
            // and catch block for our media player.
            try {
                val file = File(applicationContext.getExternalFilesDir(null), "song.mp3")
                if (file.exists()) file.delete()
                file.appendBytes(URL("http://sirdella.ddns.net:5045/dou").readBytes())
                // on below line we are setting audio
                // source as audio url on below line.
                //mediaPlayer.setDataSource("/storage/emulated/0/Android/data/com.sirdella.vaimpremote/files/song.mp3")
                mediaPlayer.setDataSource(audioUrl)
                // on below line we are
                // preparing our media player.
                mediaPlayer.prepare()

                // on below line we are
                // starting our media player.
                mediaPlayer.start()

            } catch (e: Exception) {

                // on below line we are handling our exception.
                e.printStackTrace()
            }


        }



        Log. d("cosas", "fin mainActivity")
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
