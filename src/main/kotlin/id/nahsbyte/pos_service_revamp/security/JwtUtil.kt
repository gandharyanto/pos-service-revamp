package id.nahsbyte.pos_service_revamp.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtUtil {

    @Value("\${jwt.secret}")
    private lateinit var secret: String

    @Value("\${jwt.expiration-ms}")
    private var expirationMs: Long = 86400000

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateToken(username: String, merchantId: Long, merchantUniqueCode: String?): String =
        Jwts.builder()
            .subject(username)
            .claim("merchantId", merchantId)
            .claim("merchantUniqueCode", merchantUniqueCode)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)
            .compact()

    fun generatePosToken(merchantId: Long): String =
        Jwts.builder()
            .subject(merchantId.toString())
            .claim("type", "pos")
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)
            .compact()

    /** Safely strip "Bearer " prefix; throws if header is malformed. */
    fun resolveToken(authHeader: String): String {
        require(authHeader.startsWith("Bearer ")) { "Invalid Authorization header format" }
        return authHeader.substring(7)
    }

    fun extractUsername(token: String): String = getClaims(token).subject

    fun extractMerchantId(token: String): Long =
        (getClaims(token)["merchantId"] as? Int)?.toLong()
            ?: getClaims(token)["merchantId"] as Long

    fun extractMerchantUniqueCode(token: String): String? =
        getClaims(token)["merchantUniqueCode"] as? String

    fun isTokenValid(token: String): Boolean = runCatching {
        getClaims(token).expiration.after(Date())
    }.getOrDefault(false)

    private fun getClaims(token: String): Claims =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
}
