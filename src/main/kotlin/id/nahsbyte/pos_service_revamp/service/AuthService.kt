package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.request.LoginRequest
import id.nahsbyte.pos_service_revamp.dto.response.LoginResponse
import id.nahsbyte.pos_service_revamp.exception.BusinessException
import id.nahsbyte.pos_service_revamp.repository.MerchantRepository
import id.nahsbyte.pos_service_revamp.repository.UserDetailRepository
import id.nahsbyte.pos_service_revamp.security.JwtUtil
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val jwtUtil: JwtUtil,
    private val userDetailRepository: UserDetailRepository,
    private val merchantRepository: MerchantRepository
) {

    fun login(request: LoginRequest): LoginResponse {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.username, request.password)
        )

        val userDetail = userDetailRepository.findByUsername(request.username)
            .orElseThrow { BusinessException("User detail not found") }

        val merchantId = userDetail.merchantId
            ?: throw BusinessException("User is not associated with a merchant")

        val merchant = merchantRepository.findById(merchantId)
            .orElseThrow { BusinessException("Merchant not found") }

        val token = jwtUtil.generateToken(request.username, merchantId, merchant.merchantUniqueCode)
        val posToken = jwtUtil.generatePosToken(merchantId)

        return LoginResponse(
            token = token,
            posToken = posToken,
            posKey = merchant.merchantUniqueCode
        )
    }
}
