package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.request.AdjustLoyaltyPointsRequest
import id.nahsbyte.pos_service_revamp.dto.request.CreateCustomerRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdateCustomerRequest
import id.nahsbyte.pos_service_revamp.dto.response.CustomerResponse
import id.nahsbyte.pos_service_revamp.dto.response.LoyaltyTransactionResponse
import id.nahsbyte.pos_service_revamp.entity.Customer
import id.nahsbyte.pos_service_revamp.entity.LoyaltyTransaction
import id.nahsbyte.pos_service_revamp.exception.ResourceNotFoundException
import id.nahsbyte.pos_service_revamp.repository.CustomerRepository
import id.nahsbyte.pos_service_revamp.repository.LoyaltyTransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CustomerService(
    private val customerRepository: CustomerRepository,
    private val loyaltyTransactionRepository: LoyaltyTransactionRepository
) {

    fun list(merchantId: Long, phone: String? = null, email: String? = null): List<CustomerResponse> {
        if (phone != null)
            return customerRepository.findByMerchantIdAndPhone(merchantId, phone)
                .map { listOf(it.toResponse()) }.orElse(emptyList())
        if (email != null)
            return customerRepository.findByMerchantIdAndEmail(merchantId, email)
                .map { listOf(it.toResponse()) }.orElse(emptyList())
        return customerRepository.findByMerchantId(merchantId).map { it.toResponse() }
    }

    fun detail(merchantId: Long, customerId: Long): CustomerResponse =
        customerRepository.findById(customerId)
            .filter { it.merchantId == merchantId }
            .map { it.toResponse() }
            .orElseThrow { ResourceNotFoundException("Customer not found") }

    @Transactional
    fun create(merchantId: Long, request: CreateCustomerRequest): CustomerResponse {
        val customer = Customer().apply {
            this.merchantId = merchantId
            name = request.name
            phone = request.phone
            email = request.email
            address = request.address
            gender = request.gender
        }
        return customerRepository.save(customer).toResponse()
    }

    @Transactional
    fun update(merchantId: Long, request: UpdateCustomerRequest): CustomerResponse {
        val customer = customerRepository.findById(request.id)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("Customer not found") }
        customer.apply {
            name = request.name
            phone = request.phone
            email = request.email
            address = request.address
            gender = request.gender
            isActive = request.isActive
        }
        return customerRepository.save(customer).toResponse()
    }

    @Transactional
    fun delete(merchantId: Long, customerId: Long) {
        val customer = customerRepository.findById(customerId)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("Customer not found") }
        customerRepository.delete(customer)
    }

    // ── Loyalty Points ────────────────────────────────────────────────────

    fun loyaltyHistory(merchantId: Long, customerId: Long): List<LoyaltyTransactionResponse> {
        customerRepository.findById(customerId)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("Customer not found") }
        return loyaltyTransactionRepository.findByCustomerIdOrderByCreatedDateDesc(customerId)
            .map { it.toResponse() }
    }

    @Transactional
    fun adjustPoints(merchantId: Long, username: String, request: AdjustLoyaltyPointsRequest): CustomerResponse {
        val customer = customerRepository.findById(request.customerId)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("Customer not found") }

        val newBalance = customer.loyaltyPoints + request.points
        if (newBalance < java.math.BigDecimal.ZERO)
            throw IllegalArgumentException("Insufficient loyalty points")

        customer.loyaltyPoints = newBalance
        customerRepository.save(customer)

        loyaltyTransactionRepository.save(LoyaltyTransaction().apply {
            this.merchantId = merchantId
            customerId = customer.id
            points = request.points
            type = request.type
            note = request.note
            createdBy = username
            createdDate = LocalDateTime.now()
        })

        return customer.toResponse()
    }

    // ── Mapper ────────────────────────────────────────────────────────────

    private fun Customer.toResponse() = CustomerResponse(
        id = id,
        name = name,
        phone = phone,
        email = email,
        address = address,
        gender = gender,
        loyaltyPoints = loyaltyPoints,
        totalTransaction = totalTransaction,
        totalSpend = totalSpend,
        isActive = isActive,
        createdDate = createdDate,
        modifiedDate = modifiedDate
    )

    private fun LoyaltyTransaction.toResponse() = LoyaltyTransactionResponse(
        id = id,
        customerId = customerId,
        transactionId = transactionId,
        points = points,
        type = type,
        note = note,
        createdDate = createdDate
    )
}
