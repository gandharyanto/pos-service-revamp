package id.nahsbyte.pos_service_revamp.dto.request

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank val username: String,
    @field:NotBlank val password: String
)
