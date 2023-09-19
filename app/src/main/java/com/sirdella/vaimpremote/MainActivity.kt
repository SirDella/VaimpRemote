package com.sirdella.vaimpremote

import android.animation.ValueAnimator
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.lang.Math.abs
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.system.measureTimeMillis


class MainActivity : AppCompatActivity() {

    lateinit var receiver: broadcastReceiver
    lateinit var mediaPlayer: MediaPlayer
    lateinit var app: App

    fun guardarLogFatu(mensaje: String, borrar: Boolean = false)
    {
        val path = this.getExternalFilesDir(null)
        val file = File(path, "Log.txt")

        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)
        val second = c.get(Calendar.SECOND)
        val millisecond = c.get(Calendar.MILLISECOND)


        if (!borrar) file.appendText("[$year-$month-$day $hour:$minute:$second,$millisecond] " + mensaje + "\n")
        else file.delete()
    }

    override fun onDestroy() {
        Timer().cancel()
        Log.d("cosas", "Ondestroy")
        mediaPlayer.release()
        guardarLogFatu("", true)
        super.onDestroy()
    }

    override fun onBackPressed() {
        val etBusqueda = findViewById<EditText>(R.id.etBusqueda)
        etBusqueda.visibility = View.GONE

        if (etBusqueda.text.toString() != "") rvLista.scheduleLayoutAnimation()
        adapterRecycler.actualizarLista(app!!.repoVaimp!!.listaCanciones)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("cosas", "inicio mainActivity")

        val servicioVaimp = vaimpService()
        app = (application as App)
        var playbackState = PlaybackStateDC()

        //Recycler:
        rvLista = findViewById<RecyclerView>(R.id.recyclerCanciones)
        adapterRecycler = CancionAdapter(this, callbackClick = {
            ocultarTeclado()
            val cancion = it
            app.repoVaimp!!.servicioVaimp.SelectSong(app.repoVaimp!!.mainIp!!, it.index){
                guardarLogFatu("Poner ${cancion.Name}")
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

        val bPlay = findViewById<CardView>(R.id.cardViewPlay)
        bPlay.setOnClickListener {
            servicioVaimp.PlayPause(app.repoVaimp!!.mainIp!!){
                guardarLogFatu("Play/pausa")
            }
        }

        val bNext = findViewById<CardView>(R.id.imageViewNext)
        bNext.setOnClickListener {
            servicioVaimp.NextSong(app.repoVaimp!!.mainIp!!){
                guardarLogFatu("Siguiente")
            }
        }

        val bPrev = findViewById<CardView>(R.id.imageViewPrev)
        bPrev.setOnClickListener {
            servicioVaimp.PreviousSong(app.repoVaimp!!.mainIp!!){
                guardarLogFatu("Previa (sin alcohol)")
            }
        }

        val timeSeekbar = findViewById<SeekBar>(R.id.timeseekBar)
        var dontMoveSeekbar = false
        timeSeekbar.setOnSeekBarChangeListener(object: OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser)
                {
                    dontMoveSeekbar = true
                    servicioVaimp.SetMusicPos(app.repoVaimp!!.mainIp!!, (progress*playbackState.SongLength)/timeSeekbar.max, callbackRespuesta = {
                        guardarLogFatu("Seek a ${(progress*playbackState.SongLength)/timeSeekbar.max}")
                    })
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        val tvCurrentSong = findViewById<TextView>(R.id.tvCurrentSong)

        val ivPlayPausa = findViewById<ImageView>(R.id.ivPlayPausa)

        val progressBarLogo = findViewById<ProgressBar>(R.id.progressBar)

        val etBusqueda = findViewById<EditText>(R.id.etBusqueda)
        etBusqueda.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                adapterRecycler.actualizarLista(filtrar(etBusqueda.text.toString(), app!!.repoVaimp!!.listaCanciones))
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        })

        val cvBusqueda = findViewById<CardView>(R.id.cvBusqueda)
        cvBusqueda.setOnClickListener(object: View.OnClickListener{
            override fun onClick(v: View?) {
                etBusqueda.visibility = View.VISIBLE
                etBusqueda.requestFocus()
                val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(etBusqueda, InputMethodManager.SHOW_IMPLICIT)

                if (etBusqueda.text.toString() != "") rvLista.scheduleLayoutAnimation()
                adapterRecycler.actualizarLista(filtrar(etBusqueda.text.toString(), app!!.repoVaimp!!.listaCanciones))
            }
        })


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
        var currentSong = ""
        var seekDelay=0;
        var callDelayAvg=-999;
        var callDelay=0
        var downloading = false
        var cooldown = 0
        var resync = false
        var currentPositionLast = 0
        var songPosLast = 0

        var timer = Timer().scheduleAtFixedRate(object : TimerTask() {
            @RequiresApi(Build.VERSION_CODES.N)
            override fun run()
            {
                try {callDelay = measureTimeMillis {

                        app.repoVaimp!!.servicioVaimpJson.GetPlaybackState(app.repoVaimp!!.mainIp!!, callbackResultado = {
                            if (it.Songname == "") //esto es una negrada
                            {
                                runOnUiThread {  tvCurrentSong.text = getString(R.string.string_error)}
                            }
                            playbackState = it
                        })
                    }.toInt()

                    //de acá para abajo no se ejecuta si no hay conexión por algun motivo mistico

                    runOnUiThread{
                        if (!dontMoveSeekbar) animarSeekbar(timeSeekbar.progress, ((playbackState.SongPos/playbackState.SongLength)*timeSeekbar.max).toInt(), 1000, timeSeekbar)
                        tvCurrentSong.text = playbackState.Songname

                        if (downloading)
                        {
                            progressBarLogo.visibility = View.VISIBLE
                        }
                        else if (progressBarLogo.visibility == View.VISIBLE) progressBarLogo.visibility = View.INVISIBLE
                    }

                    if (playbackState.Songname != currentSong && !downloading) {
                        downloading = true
                        Log.d("latency", "Inicio descarga ${playbackState.Songname}")

                        GlobalScope.launch {
                            try {
                                var audioUrl = "http://" + app.repoVaimp!!.mainIp + "/Stream"

                                val cancionDescargando = playbackState.Songname
                                guardarLogFatu("Descargando $cancionDescargando")

                                val file = File(
                                    applicationContext.getExternalFilesDir(null),
                                    "song.mp3"
                                )
                                if (file.exists()) file.delete()

                                val obj = URL(audioUrl)
                                val con = obj.openConnection() as HttpURLConnection
                                val outputStream = FileOutputStream(file)

                                con.requestMethod = "GET"
                                con.connectTimeout=2000
                                con.readTimeout=2000

                                val inputStream = con.inputStream

                                val buffer = ByteArray(4096)

                                var bytesRead: Int
                                while (inputStream.read(buffer).also { bytesRead = it } != -1 && playbackState.Songname == cancionDescargando) {
                                    outputStream.write(buffer, 0, bytesRead)
                                }

                                outputStream.close()
                                inputStream.close()

                                if (playbackState.Songname != cancionDescargando)
                                {
                                    guardarLogFatu("Abortada la descarga de $cancionDescargando")
                                }
                                else
                                {
                                    mediaPlayer.reset()
                                    mediaPlayer.setDataSource(file.absolutePath)
                                    //mediaPlayer.setDataSource("http://sirdella.ddns.net:2050/D%3A/Users/Lucad/Documents/The%20Great%20Unification/Backups/Moto%20G82/Almacenamiento/AUDIO/M%C3%BAsica%20Cirueliana/Mittsies%20-%20Voidreckon%20(Full%20Album).m4a")
                                    mediaPlayer.prepare()

                                    val callDelay2 = measureTimeMillis {
                                        app.repoVaimp!!.servicioVaimpJson.GetPlaybackState(app.repoVaimp!!.mainIp!!, callbackResultado = {
                                            playbackState = it
                                        })
                                    }.toInt()

                                    if (callDelayAvg<=0)
                                    {
                                        callDelayAvg = callDelay2
                                    }

                                    mediaPlayer.start()

                                    resync = true
                                    downloading = false
                                    cooldown=1
                                    currentSong = cancionDescargando

                                    guardarLogFatu("Descarga completada de $currentSong")
                                }
                            }
                            catch (e: Exception) {
                                Log.d("mediaplayer", e.toString())
                                downloading = false
                                guardarLogFatu("Excepción: ${ e.toString()}")
                            }
                        }
                    }

                    val dif = (playbackState.SongPos * 1000) - mediaPlayer.currentPosition
                    val currPosDif =  mediaPlayer.currentPosition - currentPositionLast
                    val songPosDif = (playbackState.SongPos*1000).toInt() - songPosLast

                    Log.d("latency", "dif: ${dif} seekLatency: $seekDelay, callDelayAvg: $callDelayAvg, callDelay: $callDelay, currPosDif: $currPosDif, songPosDif: ${songPosDif}")

                    if (abs(dif) > 2000)
                    {
                        resync = true
                    }

                    if (!playbackState.IsPlaying && mediaPlayer.isPlaying)
                    {
                        runOnUiThread {Glide.with(applicationContext).load(R.drawable.playicon).transition(DrawableTransitionOptions.withCrossFade()).centerCrop().into(ivPlayPausa)}

                        mediaPlayer.pause()
                    }

                    if (playbackState.IsPlaying && !mediaPlayer.isPlaying && currentSong == playbackState.Songname)
                    {
                        runOnUiThread { Glide.with(applicationContext).load(R.drawable.pausebtnom).transition(DrawableTransitionOptions.withCrossFade()).centerCrop().into(ivPlayPausa) }
                        mediaPlayer.start()
                        resync = true
                    }

                    if ((resync && (currentSong == playbackState.Songname && !downloading)))
                    {
                        reSync(playbackState)
                    }

                    if ((((playbackState.SongPos*1000) + callDelayAvg + seekDelay) - mediaPlayer.currentPosition) > seekDelay  && playbackState.IsPlaying) seekDelay++;
                    else if (seekDelay > 0 && playbackState.IsPlaying) seekDelay--;

                    if (callDelay > callDelayAvg) callDelayAvg++
                    else callDelayAvg--

                    if (cooldown>0) cooldown--

                    currentPositionLast = mediaPlayer.currentPosition
                    songPosLast = (playbackState.SongPos*1000).toInt()
                }
                catch (e: Exception)
                {
                    Log.e("cosas", e.toString())
                }

                dontMoveSeekbar = false
            }

            private fun reSync(playbackState: PlaybackStateDC) {
                if (cooldown <= 0)
                {
                    val seekTo = (playbackState.SongPos * 1000).toInt() + callDelayAvg + seekDelay
                    guardarLogFatu("reSync a $seekTo")
                    Log.d("latency", "seekTo: $seekTo")
                    mediaPlayer.seekTo(seekTo)
                    cooldown = 2
                    resync = false
                }
            }
        }, 0, 1000)

        Log.d("cosas", "fin mainActivity")
    }

    private fun filtrar(busqueda: String, lista: ArrayList<SongListDC>?): ArrayList<SongListDC> {
        val salida = ArrayList<SongListDC>()
        if (lista == null) return salida
        for(i in lista)
        {
            if (i.Name.uppercase().contains(busqueda.uppercase()))
            {
                salida.add(i)
            }
        }
        return salida
    }

    private fun ocultarTeclado() {
        val imManager =
            applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        val windowToken = currentFocus?.windowToken
        if (windowToken != null) {
            imManager?.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    private fun animarSeekbar(
        inicio: Int,
        fin: Int,
        ms: Int,
        seekbar: SeekBar
    ) {
        val anim = ValueAnimator.ofInt(inicio, fin)
        anim.duration = ms.toLong()
        anim.addUpdateListener { animation ->
            val animProgress = animation.animatedValue as Int
            if (!seekbar.isPressed)
            {
                seekbar.setProgress(animProgress)
            }
            else
            {
                anim.cancel()
            }
        }
        anim.start()
    }

    class broadcastReceiver(callback: (Boolean) -> Unit): BroadcastReceiver() {
        val callbak = callback
        override fun onReceive(context: Context?, intent: Intent?) {
            callbak(true)
        }
    }

    lateinit var adapterRecycler: CancionAdapter
    lateinit var rvLista: RecyclerView

    class CancionAdapter(private val contexto: Context?, private val callbackClick: (SongListDC) -> Unit) : RecyclerView.Adapter<CancionVH>() {

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
                Glide.with(contexto!!).load(R.drawable.defaultsongimage).transition(DrawableTransitionOptions.withCrossFade()).centerCrop().into(holder.ivImagen)
            }
            else
            {
                Glide.with(contexto!!).load(cancion.ImgUrl).transition(DrawableTransitionOptions.withCrossFade()).centerCrop().into(holder.ivImagen)
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
