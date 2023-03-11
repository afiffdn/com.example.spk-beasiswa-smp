package com.example.model

data class Response<T>(
    val data : T,
    val success : Boolean
)
