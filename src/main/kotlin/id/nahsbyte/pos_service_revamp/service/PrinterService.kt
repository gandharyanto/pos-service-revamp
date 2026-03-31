package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.request.CreatePrinterRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdatePrinterRequest
import id.nahsbyte.pos_service_revamp.dto.response.PrinterResponse
import id.nahsbyte.pos_service_revamp.entity.PrinterSetting
import id.nahsbyte.pos_service_revamp.exception.ResourceNotFoundException
import id.nahsbyte.pos_service_revamp.repository.PrinterRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PrinterService(private val printerRepository: PrinterRepository) {

    fun list(merchantId: Long, outletId: Long?): List<PrinterResponse> =
        if (outletId != null)
            printerRepository.findAllByMerchantIdAndOutletId(merchantId, outletId).map { it.toResponse() }
        else
            printerRepository.findAllByMerchantId(merchantId).map { it.toResponse() }

    fun detail(merchantId: Long, printerId: Long): PrinterResponse =
        printerRepository.findByMerchantIdAndId(merchantId, printerId)
            .map { it.toResponse() }
            .orElseThrow { ResourceNotFoundException("Printer not found") }

    @Transactional
    fun create(merchantId: Long, username: String, request: CreatePrinterRequest): PrinterResponse {
        if (request.isDefault) clearDefault(merchantId, request.outletId, request.type)

        val printer = PrinterSetting().apply {
            this.merchantId = merchantId
            outletId = request.outletId
            type = request.type
            name = request.name
            connectionType = request.connectionType
            ipAddress = request.ipAddress
            port = request.port
            paperSize = request.paperSize
            isDefault = request.isDefault
            isActive = true
            createdBy = username
            createdDate = LocalDateTime.now()
            modifiedBy = username
            modifiedDate = LocalDateTime.now()
        }
        return printerRepository.save(printer).toResponse()
    }

    @Transactional
    fun update(merchantId: Long, username: String, request: UpdatePrinterRequest): PrinterResponse {
        val printer = printerRepository.findByMerchantIdAndId(merchantId, request.printerId)
            .orElseThrow { ResourceNotFoundException("Printer not found") }

        if (request.isDefault && !printer.isDefault) clearDefault(merchantId, request.outletId, request.type)

        printer.apply {
            outletId = request.outletId
            type = request.type
            name = request.name
            connectionType = request.connectionType
            ipAddress = request.ipAddress
            port = request.port
            paperSize = request.paperSize
            isDefault = request.isDefault
            isActive = request.isActive
            modifiedBy = username
            modifiedDate = LocalDateTime.now()
        }
        return printerRepository.save(printer).toResponse()
    }

    @Transactional
    fun delete(merchantId: Long, printerId: Long) {
        val printer = printerRepository.findByMerchantIdAndId(merchantId, printerId)
            .orElseThrow { ResourceNotFoundException("Printer not found") }
        printerRepository.delete(printer)
    }

    private fun clearDefault(merchantId: Long, outletId: Long?, type: String) {
        printerRepository.findAllByMerchantId(merchantId)
            .filter { it.isDefault && it.type == type && it.outletId == outletId }
            .forEach { it.isDefault = false; printerRepository.save(it) }
    }

    private fun PrinterSetting.toResponse() = PrinterResponse(
        id = id,
        outletId = outletId,
        type = type,
        name = name,
        connectionType = connectionType,
        ipAddress = ipAddress,
        port = port,
        paperSize = paperSize,
        isDefault = isDefault,
        isActive = isActive
    )
}
