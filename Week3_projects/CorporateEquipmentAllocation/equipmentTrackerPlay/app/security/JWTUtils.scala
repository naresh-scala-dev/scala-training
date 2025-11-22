package security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.typesafe.config.ConfigFactory

import java.util.Date

object JWTUtils {


  private val config = ConfigFactory.load()

  private val secretKey: String = config.getString("jwt.secret")
  private val expiryMillis: Long = config.getLong("jwt.expiryMillis")

  private val algorithm = Algorithm.HMAC256(secretKey)

  def createToken(username: String, role: String): (String, Long) = {
    val now = new Date()
    val expiry = new Date(now.getTime + expiryMillis)

    val token = JWT.create()
      .withClaim("user", username)
      .withClaim("role", role)
      .withIssuedAt(now)
      .withExpiresAt(expiry)
      .sign(algorithm)

    (token, expiryMillis / 1000)
  }

  def verifyToken(token: String): Either[String, (String, String)] = {
    val verifier = JWT.require(algorithm).build()
    try {
      val decoded = verifier.verify(token)
      val username = decoded.getClaim("user").asString()
      val role = decoded.getClaim("role").asString()
      Right((username, role))
    } catch {
      case _: JWTVerificationException =>
        Left("Invalid or expired token")
    }
  }
}
