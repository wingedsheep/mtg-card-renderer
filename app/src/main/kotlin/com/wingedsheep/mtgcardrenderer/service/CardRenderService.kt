package com.wingedsheep.mtgcardrenderer.service

import com.microsoft.playwright.*
import com.microsoft.playwright.options.LoadState
import com.wingedsheep.mtgcardrenderer.model.ImageFormat
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

@Service
class CardRenderService(
    @param:Value("\${server.port:8080}") private val serverPort: Int
) {
    private var playwright: Playwright? = null
    private var browser: Browser? = null

    @Synchronized
    private fun getBrowser(): Browser {
        if (browser == null) {
            playwright = Playwright.create()
            browser = playwright!!.chromium().launch(
                BrowserType.LaunchOptions().setHeadless(true)
            )
        }
        return browser!!
    }

    @PreDestroy
    fun cleanup() {
        browser?.close()
        playwright?.close()
    }

    fun renderCard(cardJson: String, format: ImageFormat, scale: Int = 3): ByteArray {
        val context = getBrowser().newContext(
            Browser.NewContextOptions()
                .setDeviceScaleFactor(scale.toDouble())
                .setViewportSize(800, 1200)
        )
        val page = context.newPage()
        try {
            page.navigate("http://localhost:$serverPort/card-rendering/render.html")
            page.waitForLoadState(LoadState.NETWORKIDLE)

            // Render the card by calling the JS function
            page.evaluate(
                """(jsonStr) => {
                    const card = JSON.parse(jsonStr);
                    renderCardFromJson(card);
                }""",
                cardJson
            )

            // Wait for text resizing observers to complete
            page.waitForTimeout(800.0)

            // Screenshot the card element
            val cardElement = page.querySelector(".mtg-card")
                ?: throw IllegalStateException("Card element not found after rendering")

            val screenshotBytes = cardElement.screenshot(
                ElementHandle.ScreenshotOptions()
                    .setType(com.microsoft.playwright.options.ScreenshotType.PNG)
                    .setOmitBackground(true)
            )

            return when (format) {
                ImageFormat.PNG -> screenshotBytes
                ImageFormat.JPG -> convertFormat(screenshotBytes, "jpg", 0.95f)
            }
        } finally {
            page.close()
            context.close()
        }
    }

    private fun convertFormat(pngBytes: ByteArray, formatName: String, quality: Float): ByteArray {
        val image = ImageIO.read(ByteArrayInputStream(pngBytes))

        // For JPG, remove alpha channel
        val outputImage = if (formatName == "jpg") {
            val rgb = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
            val g = rgb.createGraphics()
            g.drawImage(image, 0, 0, java.awt.Color.WHITE, null)
            g.dispose()
            rgb
        } else {
            image
        }

        val output = ByteArrayOutputStream()
        val writers = ImageIO.getImageWritersByFormatName(formatName)
        if (!writers.hasNext()) {
            throw UnsupportedOperationException("Image format '$formatName' is not supported. You may need to add an ImageIO plugin.")
        }
        val writer = writers.next()
        val ios = ImageIO.createImageOutputStream(output)
        writer.output = ios
        val param = writer.defaultWriteParam
        if (param.canWriteCompressed()) {
            param.compressionMode = ImageWriteParam.MODE_EXPLICIT
            param.compressionQuality = quality
        }
        writer.write(null, IIOImage(outputImage, null, null), param)
        writer.dispose()
        ios.close()
        return output.toByteArray()
    }
}
