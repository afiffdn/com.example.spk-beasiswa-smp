package com.example.db.entities

import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

object UserRegistEntity : Table<Nothing>("registration") {
    val id = int("id").primaryKey()
    val username =varchar("username")
    val password = varchar("password")
    val sebagai = varchar("sebagai")
}