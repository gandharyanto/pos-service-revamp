package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.request.CreateCashierRequest
import id.nahsbyte.pos_service_revamp.dto.request.ResetCashierPasswordRequest
import id.nahsbyte.pos_service_revamp.dto.request.SetCashierPinRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdateCashierRequest
import id.nahsbyte.pos_service_revamp.dto.response.CashierResponse
import id.nahsbyte.pos_service_revamp.entity.User
import id.nahsbyte.pos_service_revamp.entity.UserDetail
import id.nahsbyte.pos_service_revamp.exception.BusinessException
import id.nahsbyte.pos_service_revamp.exception.ResourceNotFoundException
import id.nahsbyte.pos_service_revamp.repository.UserDetailRepository
import id.nahsbyte.pos_service_revamp.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CashierService(
    private val userRepository: UserRepository,
    private val userDetailRepository: UserDetailRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun list(merchantId: Long): List<CashierResponse> =
        userDetailRepository.findAllByMerchantId(merchantId).map { it.toResponse() }

    fun detail(merchantId: Long, cashierId: Long): CashierResponse =
        userDetailRepository.findByMerchantIdAndId(merchantId, cashierId)
            .map { it.toResponse() }
            .orElseThrow { ResourceNotFoundException("Cashier not found") }

    @Transactional
    fun create(merchantId: Long, request: CreateCashierRequest): CashierResponse {
        if (userRepository.existsByUsername(request.username))
            throw BusinessException("Username '${request.username}' sudah digunakan")

        val user = userRepository.save(User().apply {
            username = request.username
            password = passwordEncoder.encode(request.password)!!
            fullName = request.fullName
            email = request.email
            employeeCode = request.employeeCode
            isActive = true
            createdDate = LocalDateTime.now()
            modifiedDate = LocalDateTime.now()
        })

        val detail = userDetailRepository.save(UserDetail().apply {
            this.merchantId = merchantId
            username = user.username
            outletId = request.outletId
            createdDate = LocalDateTime.now()
            modifiedDate = LocalDateTime.now()
        })

        return detail.toResponse(user)
    }

    @Transactional
    fun update(merchantId: Long, request: UpdateCashierRequest): CashierResponse {
        val detail = userDetailRepository.findByMerchantIdAndId(merchantId, request.cashierId)
            .orElseThrow { ResourceNotFoundException("Cashier not found") }

        val user = userRepository.findByUsername(detail.username)
            .orElseThrow { ResourceNotFoundException("User not found") }

        user.apply {
            fullName = request.fullName
            email = request.email
            employeeCode = request.employeeCode
            isActive = request.isActive
            modifiedDate = LocalDateTime.now()
        }
        userRepository.save(user)

        detail.outletId = request.outletId
        detail.modifiedDate = LocalDateTime.now()
        userDetailRepository.save(detail)

        return detail.toResponse(user)
    }

    @Transactional
    fun delete(merchantId: Long, cashierId: Long) {
        val detail = userDetailRepository.findByMerchantIdAndId(merchantId, cashierId)
            .orElseThrow { ResourceNotFoundException("Cashier not found") }

        userRepository.findByUsername(detail.username).ifPresent {
            it.isActive = false
            userRepository.save(it)
        }
    }

    @Transactional
    fun setPin(merchantId: Long, request: SetCashierPinRequest) {
        val detail = userDetailRepository.findByMerchantIdAndId(merchantId, request.cashierId)
            .orElseThrow { ResourceNotFoundException("Cashier not found") }
        detail.pin = passwordEncoder.encode(request.pin)
        detail.modifiedDate = LocalDateTime.now()
        userDetailRepository.save(detail)
    }

    @Transactional
    fun resetPassword(merchantId: Long, request: ResetCashierPasswordRequest) {
        val detail = userDetailRepository.findByMerchantIdAndId(merchantId, request.cashierId)
            .orElseThrow { ResourceNotFoundException("Cashier not found") }
        val user = userRepository.findByUsername(detail.username)
            .orElseThrow { ResourceNotFoundException("User not found") }
        user.password = passwordEncoder.encode(request.newPassword)!!
        user.modifiedDate = LocalDateTime.now()
        userRepository.save(user)
    }

    private fun UserDetail.toResponse(user: User? = null): CashierResponse {
        val u = user ?: userRepository.findByUsername(username).orElse(null)
        return CashierResponse(
            id = id,
            username = username,
            fullName = u?.fullName,
            email = u?.email,
            employeeCode = u?.employeeCode,
            outletId = outletId,
            isActive = u?.isActive ?: true,
            hasPin = pin != null,
            createdDate = createdDate
        )
    }
}
