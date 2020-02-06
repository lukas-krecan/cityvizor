package digital.cesko

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.config.MapApplicationConfig
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import main.module

private val mapper by lazy { jacksonObjectMapper() }

/**
 * Set HTTP request body bytes
 */
fun TestApplicationRequest.withJson(value: Any) {
    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    setBody(mapper.writeValueAsBytes(value))
}

fun <R> runTest(test: TestApplicationEngine.() -> R): R {
    return withTestApplication({
        (environment.config as MapApplicationConfig).apply {
            put("ktor.database.jdbcUrl", "jdbc:h2:mem:test")
            put("ktor.database.driver", "org.h2.Driver")
            put("ktor.database.dbUser", "sa")
            put("ktor.database.dbPass", "")
        }
        module(testing = true)
    }, test)
}
