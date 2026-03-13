package com.wingedsheep.mtgcardrenderer.controller

import com.wingedsheep.mtgcardrenderer.model.ImageFormat
import com.wingedsheep.mtgcardrenderer.service.CardRenderService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@Tag(name = "Card Rendering", description = "Render Magic: The Gathering cards from Scryfall JSON data")
class CardRenderController(
    private val renderService: CardRenderService
) {
    @PostMapping("/render", produces = ["image/png", "image/jpeg", "image/webp"])
    @Operation(
        summary = "Render an MTG card image",
        description = "Takes a Scryfall-format card JSON and renders it as an image in the specified format.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Rendered card image",
                content = [
                    Content(mediaType = "image/png"),
                    Content(mediaType = "image/jpeg"),
                    Content(mediaType = "image/webp")
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
        return ResponseEntity.ok()
            .contentType(format.mediaType)
            .header("Content-Disposition", "inline; filename=card.${format.extension}")
            .body(imageBytes)
    }
}
