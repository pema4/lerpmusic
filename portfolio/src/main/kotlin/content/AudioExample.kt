package lerpmusic.portfolio.content

import kotlinx.html.FlowContent
import kotlinx.html.audio

enum class AudioExample(
    private val title: String,
) : ContentEntry {
    HOME_EDU_2020("2020-04-home-edu.mp3"),
    KALIMBA_2020("2020-08-kalimba.mp3"),
    DRUM_AND_BASS_2022("2022-03-drum-and-bass.mp3"),
    HIP_HOP_2022("2022-04-hip-hop.mp3"),
    ;

    private val fileName: String
        get() = "/static/audio/$title"

    override fun render(parent: FlowContent) {
        with(parent) {
            audio {
                controls = true
                autoBuffer = true
                src = fileName
            }
        }
    }
}
