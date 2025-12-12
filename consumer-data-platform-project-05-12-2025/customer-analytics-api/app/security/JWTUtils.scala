package security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import java.util.Date

object JWTUtils {
  // Secret key for HMAC256 signing - CHANGE THIS IN PRODUCTION!
  private val secretKey = "YThgvvjjhvYDYCHvjhbvjhbjhjVGVJHvjhkhbkbkhvIUGIGK"
  private val algorithm = Algorithm.HMAC256(secretKey)

  // Token expiry in milliseconds (1 hour)
  private val expiryMillis: Long = 3600 * 1000

  /**
   * Creates a JWT token with username (no role needed)
   * Returns a tuple: (token, expiresInSeconds)
   */
  def createToken(username: String): (String, Long) = {
    val now = new Date()
    val expiry = new Date(now.getTime + expiryMillis)

    val token = JWT.create()
      .withClaim("user", username)
      .withIssuedAt(now)
      .withExpiresAt(expiry)
      .sign(algorithm)

    // expires_in in seconds
    (token, expiryMillis / 1000)
  }

  /**
   * Verifies the JWT token
   * Returns Right(username) if valid, Left(errorMessage) if invalid or expired
   */
  def verifyToken(token: String): Either[String, String] = {
    val verifier = JWT.require(algorithm).build()
    try {
      val decoded = verifier.verify(token)
      val username = decoded.getClaim("user").asString()
      Right(username)
    } catch {
      case _: JWTVerificationException =>
        Left("Invalid or expired token")
    }
  }
}
