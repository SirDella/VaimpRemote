package com.sirdella.vaimpremote

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.lang.Math.abs
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis


class MainActivity : AppCompatActivity() {

    lateinit var receiver: broadcastReceiver
    lateinit var mediaPlayer: MediaPlayer
    lateinit var app: App
    var onCreated = false

    val logger = Logger(this)

    override fun onDestroy() {
        /*
        Timer().cancel()
        Log.d("cosas", "Ondestroy")
        mediaPlayer.release()
        guardarLogFatu("", true)

         */
        super.onDestroy()
    }

    override fun onBackPressed() {
        esconderBuscador()
    }

    private fun esconderBuscador() {
        val etBusqueda = findViewById<EditText>(R.id.etBusqueda)
        etBusqueda.visibility = View.GONE

        if (etBusqueda.text.toString() != "") rvLista.scheduleLayoutAnimation()
        adapterRecycler.actualizarLista(app!!.repoVaimp!!.listaCanciones)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        logger.log("Inicio MainActivity", "Flowlog")

        val servicioVaimp = VaimpCalls()
        app = (application as App)
        var playbackState = PlaybackStateDC()

        //Recycler:
        rvLista = findViewById<RecyclerView>(R.id.recyclerCanciones)
        adapterRecycler = CancionAdapter(this, callbackClick = {
            ocultarTeclado()
            val cancion = it
            app.repoVaimp!!.servicioVaimp.SelectSong(app.repoVaimp!!.mainIp!!, it.index){
                logger.log("Poner ${cancion.Name}")
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

        if (app.repoVaimp != null)
        {
            adapterRecycler.actualizarLista(app.repoVaimp!!.listaCanciones)
        }

        val bPlay = findViewById<CardView>(R.id.cardViewPlay)
        bPlay.setOnClickListener {
            servicioVaimp.PlayPause(app.repoVaimp!!.mainIp!!){
                logger.log("Play/pausa")
            }
        }

        val bNext = findViewById<CardView>(R.id.imageViewNext)
        bNext.setOnClickListener {
            servicioVaimp.NextSong(app.repoVaimp!!.mainIp!!){
                logger.log("Siguiente")
            }
        }

        val bPrev = findViewById<CardView>(R.id.imageViewPrev)
        bPrev.setOnClickListener {
            servicioVaimp.PreviousSong(app.repoVaimp!!.mainIp!!){
                logger.log("Previa (sin alcohol)")
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
                        logger.log("Seek a ${(progress*playbackState.SongLength)/timeSeekbar.max}")
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

        val tvDescarga = findViewById<TextView>(R.id.tvDescarga)


        GlobalScope.launch{
            try {
                val connectivityManager =
                    this@MainActivity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                if (networkCapabilities != null && !networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) && (z__UpdateVersion().build < URL("http://sirdella.ddns.net:8081/files/VaimpRemote/buildnumber").readText().toInt()))
                {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, getString(R.string.update_available), Toast.LENGTH_SHORT).show()
                    }

                    val file = File(applicationContext.getExternalFilesDir(null), "app.apk")
                    if (file.exists()) file.delete()

                    file.appendBytes(URL("http://sirdella.ddns.net:8081/files/VaimpRemote/app-release.apk").readBytes())

                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val uri = FileProvider.getUriForFile(
                        applicationContext,
                        applicationContext.packageName + ".provider",
                        file
                    )
                    intent.setDataAndType(uri, "application/vnd.android.package-archive")
                    applicationContext.startActivity(intent);
                }
            }
            catch(_: Exception){}
        }

        var service = ServiceVaimp()
        val intent = Intent(this, ServiceVaimp::class.java)
        intent.action = ServiceVaimp.Acciones.INICIAR.toString()
        //startService(intent)

        val mediaSession = MediaSession(this, "misesion")
        val msPlayBackState = PlaybackState.Builder().build()
        mediaSession.setPlaybackState(msPlayBackState)

        //Worker:
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()

        WorkManager.getInstance(this).cancelAllWorkByTag("vaimpchecks")
        val alarmaWorkRequest: WorkRequest =
            PeriodicWorkRequestBuilder<AlarmaWorker>(15, TimeUnit.MINUTES)
                .addTag("vaimpchecks")
                .setConstraints(constraints)
                .build()

        WorkManager
            .getInstance(applicationContext)
            .enqueue(alarmaWorkRequest)

        logger.log("Cerrando notificacion para ${app.repoVaimp!!.mainIp}", "workerlogs")
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        notificationManager.cancel(app.repoVaimp!!.mainIp.hashCode())

        //Version
        val tvVersion = findViewById<TextView>(R.id.tvVersion)
        tvVersion.text = "Build ${z__UpdateVersion().build}"

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
                        logger.log("Inicio descarga ${playbackState.Songname}", "radiolog")

                        GlobalScope.launch {
                            try {
                                var audioUrl = "http://" + app.repoVaimp!!.mainIp + "/Stream"

                                val cancionDescargando = playbackState.Songname

                                val file = File(
                                    applicationContext.getExternalFilesDir(null),
                                    "song.mp3"
                                )
                                if (file.exists()) file.delete()

                                /*
                                val obj = URL(audioUrl)
                                val con = obj.openConnection() as HttpURLConnection
                                val outputStream = FileOutputStream(file)

                                con.requestMethod = "GET"
                                con.connectTimeout=10000
                                con.readTimeout=10000

                                val inputStream = con.inputStream
                                var contador=0.00
                                val buffer = ByteArray(4096)


                                var bytesRead: Int
                                while (inputStream.read(buffer).also { bytesRead = it } != -1 && playbackState.Songname == cancionDescargando) {
                                    outputStream.write(buffer, 0, bytesRead)
                                    contador += bytesRead
                                    runOnUiThread {
                                        tvDescarga.text = "(${(((contador/1000/1000))*10.0).roundToInt()/10.0} MB)"
                                    }
                                }

                                outputStream.close()
                                inputStream.close()

                                 */

                                runOnUiThread {
                                    tvDescarga.text = ""
                                }

                                if (playbackState.Songname != cancionDescargando)
                                {
                                    logger.log("Abortada la descarga de $cancionDescargando", "radiolog")
                                }
                                else
                                {
                                    mediaPlayer.reset()
                                    //mediaPlayer.setDataSource(file.absolutePath)
                                    mediaPlayer.setDataSource(audioUrl)
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

                                    cooldown=1
                                    currentSong = cancionDescargando

                                    logger.log("Descarga completada de $currentSong", "radiolog")
                                }
                                downloading = false
                            }
                            catch (e: Exception) {
                                runOnUiThread {
                                    Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_LONG).show()
                                }
                                downloading = false
                                logger.log("Excepción: ${ e.toString()}", "radiolog")
                            }
                        }
                    }

                    val dif = (playbackState.SongPos * 1000) - mediaPlayer.currentPosition
                    val currPosDif =  mediaPlayer.currentPosition - currentPositionLast
                    val songPosDif = (playbackState.SongPos*1000).toInt() - songPosLast

                    //logger.Log("dif: ${dif} seekLatency: $seekDelay, callDelayAvg: $callDelayAvg, callDelay: $callDelay, currPosDif: $currPosDif, songPosDif: ${songPosDif}", "latencylog")

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
                    logger.log("run exception: ${e.message}", "radiolog")
                }

                dontMoveSeekbar = false
            }

            private fun reSync(playbackState: PlaybackStateDC) {
                if (cooldown <= 0)
                {
                    val seekTo = (playbackState.SongPos * 1000).toInt() + callDelayAvg + seekDelay
                    logger.log("reSync a $seekTo", "radiolog")
                    mediaPlayer.seekTo(seekTo)
                    cooldown = 2
                    resync = false
                }
            }
        }, 0, 1000)

        logger.log("Fin Mainactivity", "Flowlog")
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
