package lerpmusic.website

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import kotlin.test.Test

class ApplicationTest {
    @Test
    fun testGetConsensus() = testApplication {
        client.get("/test")
            .apply { shouldHaveStatus(200) }
            .apply { bodyAsText() shouldBe "hello" }

        client.get("/test")
            .apply { shouldHaveStatus(200) }
            .apply { bodyAsText() shouldBe "hello" }
    }
}