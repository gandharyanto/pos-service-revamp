package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.request.CloseShiftRequest
import id.nahsbyte.pos_service_revamp.dto.request.OpenShiftRequest
import id.nahsbyte.pos_service_revamp.dto.response.CashierShiftResponse
import id.nahsbyte.pos_service_revamp.entity.CashierShift
import id.nahsbyte.pos_service_revamp.exception.ResourceNotFoundException
import id.nahsbyte.pos_service_revamp.repository.CashierShiftRepository
import id.nahsbyte.pos_service_revamp.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CashierShiftService(
    private val cashierShiftRepository: CashierShiftRepository,
    private val userRepository: UserRepository
) {

    fun listByOutlet(merchantId: Long, outletId: Long, status: String? = null): List<CashierShiftResponse> {
        if (status != null)
            return cashierShiftRepository.findByMerchantIdAndOutletIdAndStatus(merchantId, outletId, status)
                .map { listOf(it.toResponse()) }.orElse(emptyList())
        return cashierShiftRepository.findByMerchantIdAndOutletId(merchantId, outletId).map { it.toResponse() }
    }

    @Transactional
    fun openShift(merchantId: Long, username: String, request: OpenShiftRequest): CashierShiftResponse {
        val user = userRepository.findByUsername(username)
            .orElseThrow { ResourceNotFoundException("User not found") }

        // Cegah satu kasir membuka shift ganda di outlet yang sama
        val existing = cashierShiftRepository.findByMerchantIdAndUserIdAndStatus(merchantId, user.id, "OPEN")
        if (existing.isPresent) throw IllegalStateException("Shift sudah terbuka untuk kasir ini")

        val shift = CashierShift().apply {
            this.merchantId = merchantId
            outletId = request.outletId
            userId = user.id
            this.username = username
            openingCash = request.openingCash
            openDate = LocalDateTime.now()
            status = "OPEN"
            note = request.note
            openedBy = username
        }
        return cashierShiftRepository.save(shift).toResponse()
    }

    @Transactional
    fun closeShift(merchantId: Long, username: String, request: CloseShiftRequest): CashierShiftResponse {
        val shift = cashierShiftRepository.findById(request.shiftId)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("Shift not found") }

        if (shift.status == "CLOSED") throw IllegalStateException("Shift sudah ditutup")

        shift.apply {
            closingCash = request.closingCash
            closeDate = LocalDateTime.now()
            status = "CLOSED"
            note = request.note ?: note
            closedBy = username
        }
        return cashierShiftRepository.save(shift).toResponse()
    }

    private fun CashierShift.toResponse() = CashierShiftResponse(
        id = id,
        merchantId = merchantId,
        outletId = outletId,
        userId = userId,
        username = username,
        openingCash = openingCash,
        closingCash = closingCash,
        openDate = openDate,
        closeDate = closeDate,
        status = status,
        note = note,
        openedBy = openedBy,
        closedBy = closedBy
    )
}
