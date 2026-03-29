package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.response.ImageUploadResponse
import id.nahsbyte.pos_service_revamp.exception.BusinessException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

@Service
class ImageService {

    @Value("\${app.upload.dir}")
    private lateinit var uploadDir: String

    private val allowedExtensions = setOf("jpg", "jpeg", "png", "gif", "webp")

    fun upload(file: MultipartFile): ImageUploadResponse {
        val originalFilename = file.originalFilename ?: throw BusinessException("Invalid file")
        val ext = originalFilename.substringAfterLast('.', "").lowercase()

        if (ext !in allowedExtensions) throw BusinessException("File type not allowed: $ext")

        val uniqueName = "${UUID.randomUUID()}.$ext"
        val thumbName = "${UUID.randomUUID()}_thumb.$ext"

        val uploadPath = Paths.get(uploadDir)
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath)

        val filePath = uploadPath.resolve(uniqueName)
        file.transferTo(filePath.toFile())

        // Copy as thumb (production: implement actual resizing)
        Files.copy(filePath, uploadPath.resolve(thumbName))

        return ImageUploadResponse(
            urlFull = "$uploadDir/$uniqueName",
            urlThumb = "$uploadDir/$thumbName"
        )
    }
}
