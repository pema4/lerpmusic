package com.example.content

import kotlinx.html.FlowContent

sealed interface ContentEntry {
    fun render(parent: FlowContent)
}

fun FlowContent.render(content: ContentEntry) {
    content.render(this)
}
