package com.example.model.siswa

data class UpdateDataSiswaRequest (
    val nisn : Int,
    val nama : String,
    val jeniskelamin : String,
    val agama : String,
    val kelas :String,
    val alamat :String
)