package com.wingedsheep.mtgcardrenderer.controller

import com.wingedsheep.mtgcardrenderer.model.ImageFormat
import com.wingedsheep.mtgcardrenderer.service.CardRenderService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.Base64

@RestController
@RequestMapping("/api")
@Tag(name = "Card Rendering", description = "Render Magic: The Gathering cards from Scryfall JSON data")
class CardRenderController(
    private val renderService: CardRenderService
) {
    @PostMapping("/render", produces = ["image/png", "image/jpeg"])
    @Operation(
        summary = "Render an MTG card image",
        description = "Takes a Scryfall-format card JSON and renders it as an image. " +
            "Card art can be referenced via a URL in image_uris.art_crop within the JSON.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Rendered card image",
                content = [
                    Content(mediaType = "image/png"),
                    Content(mediaType = "image/jpeg")
                ]
            ),
            ApiResponse(responseCode = "400", description = "Invalid card JSON"),
            ApiResponse(responseCode = "500", description = "Rendering failed")
        ]
    )
    fun renderCard(
        @RequestBody
        @Parameter(description = "Card data in Scryfall JSON format")
        cardJson: String,

        @RequestParam(defaultValue = "PNG")
        @Parameter(description = "Output image format")
        format: ImageFormat,

        @RequestParam(defaultValue = "3")
        @Parameter(description = "Render scale factor (higher = larger image, default 3 produces ~720px wide)")
        scale: Int
    ): ResponseEntity<ByteArray> {
        val imageBytes = renderService.renderCard(cardJson, format, scale)
        return imageResponse(imageBytes, format)
    }

    @PostMapping(
        "/render/upload",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = ["image/png", "image/jpeg"]
    )
    @Operation(
        summary = "Render an MTG card image with uploaded art",
        description = "Takes card JSON and an optional card art image file. " +
            "The uploaded image overrides any art URL in the JSON.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Rendered card image",
                content = [
                    Content(mediaType = "image/png"),
                    Content(mediaType = "image/jpeg")
                ]
            ),
            ApiResponse(responseCode = "400", description = "Invalid card JSON"),
            ApiResponse(responseCode = "500", description = "Rendering failed")
        ]
    )
    fun renderCardWithUpload(
        @RequestPart("card")
        @Parameter(description = "Card data in Scryfall JSON format")
        cardJson: String,

        @RequestPart("image", required = false)
        @Parameter(description = "Card art image file (PNG, JPG, or WEBP)")
        image: MultipartFile?,

        @RequestParam(defaultValue = "PNG")
        @Parameter(description = "Output image format")
        format: ImageFormat,

        @RequestParam(defaultValue = "3")
        @Parameter(description = "Render scale factor (higher = larger image, default 3 produces ~720px wide)")
        scale: Int
    ): ResponseEntity<ByteArray> {
        val finalJson = if (image != null) {
            injectImageAsDataUri(cardJson, image)
        } else {
            cardJson
        }
        val imageBytes = renderService.renderCard(finalJson, format, scale)
        return imageResponse(imageBytes, format)
    }

    private fun injectImageAsDataUri(cardJson: String, image: MultipartFile): String {
        val mimeType = image.contentType ?: "image/png"
        val base64 = Base64.getEncoder().encodeToString(image.bytes)
        val dataUri = "data:$mimeType;base64,$base64"

        // Simple JSON manipulation: inject/replace art_crop in image_uris
        val json = cardJson.trimEnd()

        // If image_uris already exists, replace art_crop
        if (json.contains("\"image_uris\"")) {
            return if (json.contains("\"art_crop\"")) {
                json.replace(Regex(""""art_crop"\s*:\s*"[^"]*""""), """"art_crop":"$dataUri"""")
            } else {
                json.replace(Regex(""""image_uris"\s*:\s*\{"""), """"image_uris":{"art_crop":"$dataUri",""")
            }
        }

        // No image_uris block — add one before the closing brace
        return json.replaceLast("}", ""","image_uris":{"art_crop":"$dataUri"}}""")
    }

    private fun String.replaceLast(old: String, new: String): String {
        val index = lastIndexOf(old)
        if (index < 0) return this
        return substring(0, index) + new + substring(index + old.length)
    }

    private fun imageResponse(imageBytes: ByteArray, format: ImageFormat): ResponseEntity<ByteArray> {
        return ResponseEntity.ok()
            .contentType(format.mediaType)
            .header("Content-Disposition", "inline; filename=card.${format.extension}")
            .body(imageBytes)
    }
}
