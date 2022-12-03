package lerpmusic.portfolio

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import lerpmusic.portfolio.config.configureRouting
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            configureRouting()
        }

        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }
}