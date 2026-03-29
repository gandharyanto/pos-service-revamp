package id.nahsbyte.pos_service_revamp.dto.response

data class CategoryResponse(
    val id: Long,
    val name: String,
    val image: String?,
    val description: String?
)
