package com.example.db.entities

import com.example.db.entities.AlternatifEntity.primaryKey
import org.ktorm.schema.Table
import org.ktorm.schema.double
import org.ktorm.schema.int
import org.ktorm.schema.varchar

object Alternatif2Entity : Table<Nothing> ("alternatif2") {
    val id = int ("id").primaryKey()
    val nisn =int("nisn")
    val nama = varchar("nama")
    val pend_agama = double("pend_agama")
    val pend_pancasila = double("pend_pancasila")
    val bahasa_indo = double("bahasa_indo")
    val mtk = double("mtk")
    val sejarah_indo = double("sejarah_indo")
    val bahasa_ing = double("bahasa_ing")
    val seni_budaya = double("seni_budaya")
    val penjas = double("penjas")
    val prakarya_dan_kwh = double("prakarya_dan_kwh")
    val biologi = double("biologi")
    val fisika = double("fisika")
    val kimia = double("kimia")
    val sosiologi = double("sosiologi")
    val ekonomi = double("ekonomi")
    val ket_pend_agama = double("ket_pend_agama")
    val ket_pend_pancasila = double("ket_pend_pancasila")
    val ket_bahasa_indo  = double("ket_bahasa_indo")
    val ket_matematika = double("ket_matematika")
    val ket_sejarah_indo = double("ket_sejarah_indo")
    val ket_bahasa_ing = double("ket_bahasa_ing")
    val ket_seni_budaya = double("ket_seni_budaya")
    val ket_penjas = double("ket_penjas")
    val ket_prakarya_dan_kwh = double("ket_prakarya_dan_kwh")
    val ket_biologi  = double("ket_biologi")
    val ket_fisika = double("ket_fisika")
    val ket_kimia = double("ket_kimia")
    val ket_sosiologi = double("ket_sosiologi")
    val ket_ekonomi = double("ket_ekonomi")
    val pramuka = double("pramuka")
    val olahraga = double("olahraga")
    val pmr = double("pmr")
    val sikap_spiritual = double("sikap_spiritual")
    val sikap_sosial = double("sikap_sosial")
    val sakit = double("sakit")
    val izin = double("izin")
    val tanpa_keterangan = double("tanpa_keterangan")
}