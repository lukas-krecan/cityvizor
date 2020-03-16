package full_text_search.service

import full_text_search.model.InvoiceLucineModel
import full_text_search.model.Payments
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.WildcardQuery
import org.apache.lucene.store.RAMDirectory
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

const val MAX_SIZE = 100

const val PROFILE = "profile"
const val COUNTERPARTY_ID = "counterpartyId"
const val COUNTERPARTY_NAME = "counterpartyName"
const val DESCRIPTION = "description"
const val ITEM = "id"


object SearchService {
    private val memoryIndex = RAMDirectory()

    // Init on startup, should have ability to reload index, not implemented yet
    init {
        val analyzer = StandardAnalyzer()
        val indexWriterConfig = IndexWriterConfig(analyzer)
        IndexWriter(memoryIndex, indexWriterConfig).use { writer ->
            transaction {
                Payments.selectAll().forEach {
                    val document = Document()
                    document.add(TextField(ITEM, it[Payments.item].toString(), Field.Store.YES))
                    document.add(TextField(PROFILE, it[Payments.profileId].toString(), Field.Store.YES))
                    document.add(TextField(COUNTERPARTY_ID, it[Payments.counterpartyId].toString(), Field.Store.YES))
                    document.add(TextField(COUNTERPARTY_NAME, it[Payments.counterpartyName], Field.Store.YES))
                    document.add(TextField(DESCRIPTION, it[Payments.description], Field.Store.YES))
                    writer.addDocument(document)
                }
                commit()
            }
        }
    }

    fun search(query: String, profile: String?): List<InvoiceLucineModel> {
        val counterpartyIdQuery = WildcardQuery(Term(COUNTERPARTY_ID, "$query*".toLowerCase()))
        val counterpartyNameQuery = WildcardQuery(Term(COUNTERPARTY_NAME, "$query*".toLowerCase()))
        val descriptionQuery = WildcardQuery(Term(DESCRIPTION, "$query*".toLowerCase()))
        val chainQueryBuilderForFulltext = BooleanQuery.Builder()
        chainQueryBuilderForFulltext.add(counterpartyIdQuery, BooleanClause.Occur.SHOULD)
        chainQueryBuilderForFulltext.add(counterpartyNameQuery, BooleanClause.Occur.SHOULD)
        chainQueryBuilderForFulltext.add(descriptionQuery, BooleanClause.Occur.SHOULD)
        val finalQuery = chainQueryBuilderForFulltext.build()
        val indexReader: IndexReader = DirectoryReader.open(memoryIndex)
        val searcher = IndexSearcher(indexReader)
        val topDocs = searcher.search(finalQuery, MAX_SIZE)
        val mapped = topDocs.scoreDocs.map {
            val doc = searcher.doc(it.doc)
            val result = InvoiceLucineModel(
                    id = doc.get(ITEM).toInt(),
                    profile = doc.get(PROFILE),
                    description = doc.get(DESCRIPTION),
                    counterPartyId = doc.get(COUNTERPARTY_ID),
                    counterPartyName = doc.get(COUNTERPARTY_NAME)
            )
            result
        }
        return if (profile == null) mapped else mapped.filter { it.profile == profile }
    }
}

