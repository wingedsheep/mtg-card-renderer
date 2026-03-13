package com.wingedsheep.mtgcardrenderer.model

import org.springframework.http.MediaType

enum class ImageFormat(val mediaType: MediaType, val extension: String) {
    PNG(MediaType.IMAGE_PNG, "png"),
    JPG(MediaType("image", "jpeg"), "jpg");
}
