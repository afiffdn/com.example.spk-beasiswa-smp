package com.example.model


import org.mindrot.jbcrypt.BCrypt

data class UserCredential(
    val username :String,
    val password :String,
    val sebagai :String
){
    fun hashedPassword() : String{
        return BCrypt.hashpw(password,BCrypt.gensalt())
    }
    fun isValidCredentials():Boolean{
        return username.length >= 3 && password.length >=6
    }
}
