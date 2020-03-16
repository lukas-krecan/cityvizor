package main

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.typesafe.config.ConfigFactory
import digital.cesko.city_request.cityRequestRouter
import digital.cesko.city_request.google_sheets.GoogleSheets
import digital.cesko.city_sync.CitySynchronizationService
import digital.cesko.city_sync.citySynchronizationRouter
import digital.cesko.city_sync.exception.CitySyncException
import digital.cesko.common.CommonConfig
import digital.cesko.routers.citySearchRouter
import full_text_search.fullTextSearchRouter
import full_text_search.model.Invoices
import full_text_search.service.import
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
@KtorExperimentalAPI
fun Application.module(testing: Boolean = false) {

    val configuration = ConfigFactory.load("application.conf").resolve()

    val config = CommonConfig(
            configuration.getConfig("app.city-sync.instanceUrls").entrySet()
                    .associateBy({ it.key }, { it.value.unwrapped().toString() })
    )

    install(Authentication) {
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            registerModule(JodaModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    val jdbcUrl = environment.config.property("ktor.database.jdbcUrl").getString()
    val driver = environment.config.property("ktor.database.driver").getString()
    val dbUser = environment.config.property("ktor.database.dbUser").getString()
    val dbPass = environment.config.property("ktor.database.dbPass").getString()
    Database.connect(
            jdbcUrl, driver = driver,
            user = dbUser, password = dbPass
    )

    install(XForwardedHeaderSupport)    // city request logs remote IP

    transaction {
        SchemaUtils.drop(Invoices)
    }
    routing {
        fullTextSearchRouter()
        citySearchRouter()
        cityRequestRouter(
                configuration.getString("app.city-request.timeZone"),
                GoogleSheets(
                        System.getProperty("googleCredentials"),
                        configuration.getString("app.city-request.sheetId"),
                        configuration.getString("app.city-request.listName"),
                        configuration.getString("app.city-request.appName")
                )
        )
        citySynchronizationRouter(
                CitySynchronizationService(config)
        )
        healthCheck()
    }

    import()
    install(StatusPages) {
        exception<CitySyncException> {
            call.respond(it.status ?: HttpStatusCode.InternalServerError, it)
        }
    }
}



