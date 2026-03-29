package id.nahsbyte.pos_service_revamp.dto.response

data class LoginResponse(
    val token: String,
    val posToken: String,
    val posKey: String?
)
