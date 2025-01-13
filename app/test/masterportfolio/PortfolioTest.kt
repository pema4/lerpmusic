package lerpmusic.website.masterportfolio

import io.ktor.client.request.*
import io.ktor.http.*
import lerpmusic.website.testLerpMusicApplication
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PortfolioTest {
    @Test
    fun testMasterPortfolio() = testLerpMusicApplication {
        client.get("/master-portfolio").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }
}
