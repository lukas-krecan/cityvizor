package digital.cesko.full_text_search

import digital.cesko.AbstractKtorTest
import digital.cesko.sendRequest
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import kotlin.test.Test
import kotlin.test.assertEquals

class FullTextSearchTest : AbstractKtorTest() {
    @Test
    fun `Should search invoices`() {
        runTest {
            sendRequest(HttpMethod.Get, "/api/v2/service/search?query").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertThatJson(response.content!!).isArray().contains("""{"id" : 6, "name" : "Praha 3", "ico" : "00063517"}""")
            }
        }
    }
}
