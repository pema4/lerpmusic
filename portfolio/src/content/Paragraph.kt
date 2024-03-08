package lerpmusic.portfolio.content

import kotlinx.html.FlowContent
import kotlinx.html.p

data class Paragraph(val text: String) : ContentEntry {
    override fun render(parent: FlowContent) {
        parent.render(this)
    }
}

private fun FlowContent.render(paragraph: Paragraph) {
    p {
        +paragraph.text
    }
}
