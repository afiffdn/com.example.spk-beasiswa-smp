package com.example.db.entities

import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

object UserEntity :Table<Nothing>("siswa") {
    val nisn = int("nisn").primaryKey()
    val nama = varchar("nama")
    val jeniskelamin = varchar("jeniskelamin")
    val agama = varchar("agama")
    val kelas = varchar("kelas")
    val alamat = varchar("alamat")
}