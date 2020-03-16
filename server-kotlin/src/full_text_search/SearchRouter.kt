package full_text_search

import full_text_search.model.FTSException
import full_text_search.service.SearchService
import full_text_search.service.combineLucineAndDBData
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get

fun Routing.fullTextSearchRouter() {
    get("/api/v2/service/search") {
        val query = call.request.queryParameters["query"] ?: throw FTSException("No query supplied")
        val profile = call.request.queryParameters["profile"]
        val lucine = SearchService.search(query, profile)
        call.respond(HttpStatusCode.OK, combineLucineAndDBData(lucine))
    }
}
