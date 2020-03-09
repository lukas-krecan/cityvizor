package digital.cesko.full_text_search

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import full_text_search.memoryIndex
import full_text_search.model.Invoices
import full_text_search.service.COUNTERPARTY_ID
import full_text_search.service.COUNTERPARTY_NAME
import full_text_search.service.DESCRIPTION
import full_text_search.service.ID
import full_text_search.service.PROFILE
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.TextField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

const val FILE_NAME = "invoices.csv"

val allowedFieldsToBeExported = listOf(ID, PROFILE, COUNTERPARTY_ID, COUNTERPARTY_NAME, DESCRIPTION)

fun import() {
    val analyzer = StandardAnalyzer()
    val indexWriterConfig = IndexWriterConfig(analyzer)
    val writer = IndexWriter(memoryIndex, indexWriterConfig)
    transaction {
        SchemaUtils.create(Invoices)
        csvReader {
            delimiter = ';'
        }.open(FILE_NAME) {
            readAllWithHeader().forEach {
                val document = Document()
                it.filter { allowedFieldsToBeExported.contains(it.key) }.forEach {
                    document.add(TextField(it.key, it.value, Field.Store.YES))
                }
                Invoices.insert { invoice ->
                    invoice[id] = it["id"]!!.toInt()
                    invoice[profile] = it["profile"]!!
                    invoice[year] = it["year"]!!.toInt()
                    invoice[event] = it["event"]
                    invoice[eventSrcId] = it["event_src_id"]
                    invoice[type] = it["type"]!!
                    invoice[item] = it["item"]!!.toInt()
                    invoice[paragraph] = it["item"]!!.toInt()
                    invoice[date] = DateTime.parse(it["date"]!!)
                    invoice[amount] = it["amount"]!!.toDouble()
                    invoice[counterpartyId] = it["counterparty_id"]?.toInt()
                    invoice[counterpartyName] = it["counterparty_name"]
                    invoice[description] = it["description"]!!
                }
                writer.addDocument(document)
            }
        }
        writer.close()
        commit()
    }
}

