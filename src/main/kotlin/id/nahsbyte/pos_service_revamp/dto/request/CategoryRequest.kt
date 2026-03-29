package id.nahsbyte.pos_service_revamp.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class AddCategoryRequest(
    @field:NotBlank val name: String,
    val image: String? = null,
    val description: String? = null
)

data class UpdateCategoryRequest(
    @field:NotNull val categoryId: Long,
    @field:NotBlank val name: String,
    val image: String? = null,
    val description: String? = null
)
