package lerpmusic.website

import io.ktor.server.testing.*
import kotlinx.coroutines.test.TestResult

fun testLerpMusicApplication(block: suspend ApplicationTestBuilder.() -> Unit): TestResult =
    testApplication {
        lerpMusicApplication()
    }

fun TestApplicationBuilder.lerpMusicApplication() {
    application {
        lerpMusicModule()
    }
}