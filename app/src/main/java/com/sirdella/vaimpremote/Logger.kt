package com.sirdella.vaimpremote

import android.content.Context
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import android.util.Log

class Logger(private val contexto: Context) {
    private var listaTags = ArrayList<String>()

    init {
        cargarTags()
    }

    fun log(mensaje: String, tag: String = "logs")
    {
        val path = contexto.getExternalFilesDir(null)
        val file = File(path, "allLogs.txt")
        val file2 = File(path, tag + ".txt")

        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)
        val second = c.get(Calendar.SECOND)
        val millisecond = c.get(Calendar.MILLISECOND)

        file.appendText("[$year-$month-$day $hour:$minute:$second,$millisecond] $mensaje\n")
        file2.appendText("[$year-$month-$day $hour:$minute:$second,$millisecond] $mensaje\n")
        Log.d(tag, mensaje)

        addTag(tag)
    }

    fun DeleteLogs()
    {
        val path = contexto.getExternalFilesDir(null)
        val file = File(path, "allLogs.txt")
        file.delete()

        cargarTags()

        for(tag in listaTags)
        {
            val file2 = File(path,"$tag.txt")
            file2.delete()
        }
    }

    private fun cargarTags()
    {
        try {
            val path = contexto.getExternalFilesDir(null)
            val file = File(path, "tagList.txt")

            val lines = file.readLines()

            for(line in lines)
            {
                listaTags.add(line)
            }
        }
        catch (_: Exception){
        }
    }

    private fun addTag(newtag: String)
    {
        cargarTags()
        for(tag in listaTags)
        {
            if (tag == newtag) return
        }
        val path = contexto.getExternalFilesDir(null)
        val file = File(path, "tagList.txt")

        file.appendText(newtag + "\n")


        listaTags.add(newtag)
    }
}