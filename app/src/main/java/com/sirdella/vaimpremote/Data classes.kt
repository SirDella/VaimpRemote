package com.sirdella.vaimpremote

data class PlaybackStateDC(
    var IsPlaying: Boolean = false,
    var Songname: String = "",
    var SongLength: Float = 0f,
    var SongPos: Float = 0f,
    var Volume: Float = 0f,
    var PlaylistUpdated: Boolean = false
)

data class SongListDC (
    var Name: String = "",
    var ImgUrl: String = "",
    var index: Int = -1
)

data class ipListDC (
    var adress: String = "",
    var online: Boolean = false
)
