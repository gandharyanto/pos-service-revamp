package id.nahsbyte.pos_service_revamp.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class AddProductRequest(
    @field:NotBlank val name: String,
    @field:NotNull val price: BigDecimal,
    val sku: String? = null,
    val upc: String? = null,
    val imageUrl: String? = null,
    val imageThumbUrl: String? = null,
    val description: String? = null,
    val qty: Int = 0,
    val categoryIds: List<Long> = emptyList()
)

data class UpdateProductRequest(
    @field:NotNull val productId: Long,
    @field:NotBlank val name: String,
    @field:NotNull val price: BigDecimal,
    val sku: String? = null,
    val upc: String? = null,
    val imageUrl: String? = null,
    val imageThumbUrl: String? = null,
    val description: String? = null,
    val categoryIds: List<Long> = emptyList()
)
