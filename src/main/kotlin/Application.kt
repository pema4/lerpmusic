package com.example

import com.example.plugins.configureCallLogging
import com.example.plugins.configureHttpsRedirect
import com.example.plugins.configureRouting
import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>): Unit =
    EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    configureRouting()
    configureCallLogging()
    configureHttpsRedirect()
}
