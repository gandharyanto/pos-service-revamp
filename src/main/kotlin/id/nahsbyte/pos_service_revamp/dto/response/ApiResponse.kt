package id.nahsbyte.pos_service_revamp.dto.response

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
) {
    companion object {
        fun <T> ok(data: T, message: String = "Success") = ApiResponse(true, message, data)
        fun <T> ok(message: String = "Success") = ApiResponse<T>(true, message, null)
        fun <T> error(message: String) = ApiResponse<T>(false, message, null)
        fun <T> error(message: String, data: T) = ApiResponse(false, message, data)
    }
}
