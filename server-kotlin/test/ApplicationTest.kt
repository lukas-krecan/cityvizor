package digital.cesko

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import digital.cesko.city_request.CityRequest
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    private val mapper by lazy { jacksonObjectMapper() }

    @Test
    fun testCityRequest() {
       runTest {
            handleRequest(HttpMethod.Post, "/api/v2/service/cityrequest") {
                withJson(CityRequest(
                    city = "Humpolec",
                    email = "starosta@humpolec.cz",
                    name = "Pan Starosta",
                    subscribe = true,
                    gdpr = true
                ))
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("ok", response.content)
            }
        }
    }
}
