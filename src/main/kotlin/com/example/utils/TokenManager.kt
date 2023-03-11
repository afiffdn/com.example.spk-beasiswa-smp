package com.example.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.example.model.UserLogin
import io.ktor.server.config.*
import java.util.*

class TokenManager(val config : HoconApplicationConfig) {
    val audience = config.property("audience").getString()
    val secret = config.property("secret").getString()
    val issuer = config.property("issuer").getString()
    val expirationDate =System.currentTimeMillis() + 86400000;


    fun generateJWTToken(user : UserLogin) : String{
        val token = JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("id", user.id)
            .withClaim("username",user.username)
            .withClaim("sebagai",user.sebagai)
            .withExpiresAt(Date(expirationDate))
            .sign(Algorithm.HMAC256(secret))

        return token
    }
fun verifyJWTToken(): JWTVerifier{
    return JWT.require(Algorithm.HMAC256(secret))
        .withAudience(audience)
        .withIssuer(issuer)
        .build()
}

}