package lerpmusic.website.masterportfolio

import kotlinx.html.FlowContent
import kotlinx.html.p

data class Paragraph(val text: String) : ContentEntry {
    override fun FlowContent.render() {
        p {
            +text
        }
    }
}
