package io.disruptedsystems.libdtn.module.core.http

import io.disruptedsystems.libdtn.core.api.CoreApi
import io.disruptedsystems.libdtn.core.spi.CoreModuleSpi
import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*

/**
 * @author Lucien Loiseau on 04/02/21.
 */
class ModuleHTTPDaemon : CoreModuleSpi {
    private val TAG = "http"

    private lateinit var api: CoreApi

    override fun getModuleName() = TAG

    override fun init(api: CoreApi?) {
        this.api = api!!

        val server = embeddedServer(Netty, port = 8080) {
            mainModule()
        }
        server.start(wait = true)
    }
}

fun Application.mainModule() {
    install(WebSockets)
    routing {
        webSocket("/ws") {
            for (frame in incoming) {
                frame as? Frame.Text ?: continue
                val receivedText = frame.readText()
                send("You said: $receivedText")
            }
        }
    }
}