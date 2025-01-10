package lerpmusic.website.masterportfolio

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class PortfolioTest {
    @Test
    fun testMasterPortfolio() = testApplication {
        client.get("/master-portfolio").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }
}