package full_text_search.model

import org.jetbrains.exposed.sql.Table

/**
CREATE TABLE data.payments (
profile_id integer,
year integer,
paragraph integer,
item integer,
unit integer,
event bigint,
amount numeric(14,2),
date date,
counterparty_id character varying,
counterparty_name text,
description text
);
 */

object Payments : Table() {
    val profileId = integer("profileId")
    val year = integer("year")
    val paragraph = integer("paragraph")
    val item = integer("item")
    val unit = integer("unit").nullable()
    val event = long("event").nullable()
    val amount = decimal("amount", 14, 2)
    val date = date("date")
    val counterpartyId = varchar("counterparty_id", 50).nullable()
    val counterpartyName = text("counterparty_name").nullable()
    val description = text("description")
}
