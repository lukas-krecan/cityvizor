package digital.cesko

import city_sync.model.SyncTask
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncTest {

    @Test
    fun testSyncUnknown() {
        runTest {
            handleRequest(HttpMethod.Post, "/api/v1/citysync/synchronization") {
                withJson(
                    SyncTask(
                        instance = "unknown",
                        cityId = 123
                    )
                )
            }.apply {
                // Should be bad request
                assertEquals(HttpStatusCode.InternalServerError, response.status())
            }
        }
    }
}
