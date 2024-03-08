package lerpmusic.consensus.test

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.testApplication
import kotlin.test.Test


class RoutingKtTest {

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