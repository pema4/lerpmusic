package lerpmusic.website.portfolio

import kotlinx.html.FlowContent

interface ContentEntry {
    fun FlowContent.render()
}

fun FlowContent.render(content: ContentEntry) {
    content.run { render() }
}
