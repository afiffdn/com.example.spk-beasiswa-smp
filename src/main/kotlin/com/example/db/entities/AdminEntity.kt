package com.example.db.entities

import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

object AdminEntity : Table<Nothing>("admin") {
    val id = int("id").primaryKey()
    val nama = varchar("nama")
    val nip = int("nip")
    val jabatan = varchar("jabatan")
}