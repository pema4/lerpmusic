package lerpmusic.portfolio

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testMasterPortfolio() = testApplication {
        application {
            portfolioModule()
        }

        client.get("/master-portfolio").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }
}
