package lerpmusic.website.portfolio

import kotlinx.html.FlowContent
import kotlinx.html.iframe
import kotlinx.html.p

enum class YoutubeVideo(
    val code: String,
) : ContentEntry {
    MULTIMOD_SYNTH("sRGyNg3hKn8"),
    PROGRAMMABLE_SYNTH("vcLeD-igvMo"),
    SCALESYNTH("N_FDOf2PNL4"),
    MUSICBX_EDITOR("HmA9xVxc1fE"),
    ;

    override fun FlowContent.render() {
        p("youtube-embed") {
            iframe {
                width = "560"
                height = "315"
                src = "https://www.youtube.com/embed/$code"
                with(attributes) {
                    put("title", "YouTube video player")
                    put(
                        "allow",
                        listOf(
                            "accelerometer",
                            "autoplay",
                            "clipboard-write",
                            "encrypted-media",
                            "gyroscope",
                            "picture-in-picture"
                        ).joinToString("; ")
                    )
                    put("allowfullscreen", "allowfullscreen")
                }
            }
        }
    }
}
