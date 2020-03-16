package full_text_search.service

import full_text_search.model.InvoiceLucineModel
import full_text_search.model.InvoiceResultModel
import full_text_search.model.Payments
import full_text_search.model.Payments.amount
import full_text_search.model.Payments.counterpartyId
import full_text_search.model.Payments.counterpartyName
import full_text_search.model.Payments.date
import full_text_search.model.Payments.description
import full_text_search.model.Payments.event
import full_text_search.model.Payments.eventSrcId
import full_text_search.model.Payments.id
import full_text_search.model.Payments.item
import full_text_search.model.Payments.paragraph
import full_text_search.model.Payments.profileId
import full_text_search.model.Payments.type
import full_text_search.model.Payments.year
import full_text_search.model.ResultModel
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun combineLucineAndDBData(lucineData: List<InvoiceLucineModel>): ResultModel {
    val ids = lucineData.map { it.id }
    return ResultModel(
            transaction {
                Payments.select {
                    id.inList(ids)
                }.map { invoice ->
                    InvoiceResultModel(
                            id = invoice[id],
                            profile = invoice[profileId],
                            year = invoice[year],
                            event = invoice[event],
                            eventSrcId = invoice[eventSrcId],
                            type = invoice[type],
                            item = invoice[item],
                            paragraph = invoice[paragraph],
                            date = invoice[date].toLocalDate(),
                            amount = invoice[amount],
                            counterpartyId = invoice[counterpartyId],
                            counterpartyName = invoice[counterpartyName],
                            description = invoice[description]
                    )
                }
            }
    )
}
