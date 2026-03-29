package id.nahsbyte.pos_service_revamp.exception

import id.nahsbyte.pos_service_revamp.dto.response.AmountMismatchResponse
import id.nahsbyte.pos_service_revamp.dto.response.ApiResponse
import id.nahsbyte.pos_service_revamp.dto.response.MismatchDetail
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

class ResourceNotFoundException(message: String) : RuntimeException(message)
class BusinessException(message: String) : RuntimeException(message)
class AmountMismatchException(val mismatches: List<MismatchDetail>) :
    RuntimeException("Transaction amount mismatch: ${mismatches.size} field(s) do not match")

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.message ?: "Not found"))

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(ex: BusinessException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.message ?: "Bad request"))

    @ExceptionHandler(AmountMismatchException::class)
    fun handleAmountMismatch(ex: AmountMismatchException): ResponseEntity<ApiResponse<AmountMismatchResponse>> =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiResponse.error(ex.message ?: "Amount mismatch", AmountMismatchResponse(ex.mismatches)))

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentials(ex: BadCredentialsException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid username or password"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val errors = ex.bindingResult.allErrors.joinToString(", ") { error ->
            if (error is FieldError) "${error.field}: ${error.defaultMessage}"
            else error.defaultMessage ?: "Validation error"
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(errors))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ex.message ?: "Internal server error"))
}
