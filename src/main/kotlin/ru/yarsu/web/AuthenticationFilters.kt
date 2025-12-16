package ru.yarsu.web

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.with
import ru.yarsu.EquipmentStorage
import ru.yarsu.UserRole
import java.util.UUID

fun authFilter(
    storage: EquipmentStorage,
    secret: String,
): Filter =
    Filter { next: HttpHandler ->
        { req: Request ->
            val authHeader = req.header("Authorization")
            var requestWithAuth = req

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                val token = authHeader.removePrefix("Bearer ").trim()

                val userId = verifyJwtAndGetUserId(token, secret)

                if (userId != null) {
                    val user = storage.getUser(userId)

                    if (user != null) {
                        requestWithAuth =
                            requestWithAuth.with(
                                currentUserLens of user,
                            )
                    }
                }
            }
            next(requestWithAuth)
        }
    }

private fun verifyJwtAndGetUserId(
    token: String,
    secret: String,
): UUID? {
    return try {
        val algorithm = Algorithm.HMAC512(secret)

        val verifier =
            JWT
                .require(algorithm)
                .build()

        val decodedJWT = verifier.verify(token)

        // Проверка exp - обязательное поле
        val expClaim = decodedJWT.getClaim("exp")
        if (expClaim.isNull) {
            return null
        }

        val exp = expClaim.asLong() ?: return null

        val now =
            java.time.Instant
                .now()
                .epochSecond
        if (exp < now) {
            return null // Токен истек
        }

        val subject = decodedJWT.subject ?: return null

        UUID.fromString(subject)
    } catch (exception: JWTVerificationException) {
        null
    } catch (e: Exception) {
        null
    }
}
