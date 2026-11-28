package kr.byeongmin.watchtower.domain.tower.vo

import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ScreenshotTest {
    private fun screenshot(width: Int = 200, height: Int = 300, draw: Graphics2D.() -> Unit = {}): Screenshot {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = Color.WHITE
        graphics.fillRect(0, 0, width, height)
        graphics.draw()
        graphics.dispose()

        val png = ByteArrayOutputStream()
        ImageIO.write(image, "png", png)
        return Screenshot.from(png.toByteArray())
    }

    private fun Graphics2D.drawContent(top: Int, contentId: Int) {
        repeat(CONTENT_HEIGHT) { rowInContent ->
            val grayUniqueToContentAndRow = contentId * CONTENT_HEIGHT + rowInContent
            drawRowThatIsNotBackground(top + rowInContent, Color(grayUniqueToContentAndRow, grayUniqueToContentAndRow, grayUniqueToContentAndRow))
        }
    }

    private fun Graphics2D.drawRowThatIsNotBackground(y: Int, rowColor: Color) {
        color = rowColor
        drawLine(CONTENT_LEFT, y, CONTENT_RIGHT, y)
        color = Color.BLACK
        (CONTENT_LEFT..CONTENT_RIGHT step 8).forEach { x -> drawLine(x, y, x, y) }
    }

    private fun Screenshot.highlightedRows(): Set<Int> {
        val image = ImageIO.read(ByteArrayInputStream(toPng()))
        return (0 until image.height).filter { y ->
            (0 until image.width).any { x -> isPaintedRed(Color(image.getRGB(x, y))) }
        }.toSet()
    }

    private fun isPaintedRed(color: Color): Boolean {
        return color.red - color.green > 60
    }

    @Test
    fun `같은 스크린샷끼리 비교하면 아무 곳도 칠하지 않음`() {
        /** given */
        val previous = screenshot { drawContent(top = 100, contentId = 1) }
        val current = screenshot { drawContent(top = 100, contentId = 1) }

        /** when */
        current.highlightChangesFrom(previous)

        /** then */
        assertEquals(emptySet(), current.highlightedRows())
    }

    @Test
    fun `위에 내용이 추가돼 아래 내용이 밀려도 추가된 내용이 걸친 칸만 칠함`() {
        /** given */
        val previous = screenshot(height = 300) { drawContent(top = 100, contentId = 1) }
        val current = screenshot(height = 340) {
            drawContent(top = 10, contentId = 2)
            drawContent(top = 140, contentId = 1)
        }

        /** when */
        current.highlightChangesFrom(previous)

        /** then */
        assertEquals((0..63).toSet(), current.highlightedRows())
    }

    @Test
    fun `높이는 그대로고 내용만 페이지 다른 곳에 있는 내용으로 바뀌어도 바뀐 내용이 걸친 칸을 칠함`() {
        /** given */
        val previous = screenshot {
            drawContent(top = 40, contentId = 1)
            drawContent(top = 100, contentId = 2)
            drawContent(top = 160, contentId = 1)
        }
        val current = screenshot {
            drawContent(top = 40, contentId = 1)
            drawContent(top = 100, contentId = 1)
            drawContent(top = 160, contentId = 1)
        }

        /** when */
        current.highlightChangesFrom(previous)

        /** then */
        assertEquals((96..159).toSet(), current.highlightedRows())
    }

    @Test
    fun `내용이 사라지기만 하면 현재 스크린샷에는 칠하지 않음`() {
        /** given */
        val previous = screenshot(height = 300) {
            drawContent(top = 40, contentId = 1)
            drawContent(top = 140, contentId = 2)
        }
        val current = screenshot(height = 240) { drawContent(top = 80, contentId = 2) }

        /** when */
        current.highlightChangesFrom(previous)

        /** then */
        assertEquals(emptySet(), current.highlightedRows())
    }

    @Test
    fun `바뀐 칸은 원래 화면이 비치도록 반투명한 빨간색으로 칠함`() {
        /** given */
        val previous = screenshot()
        val current = screenshot { drawContent(top = 100, contentId = 1) }

        /** when */
        current.highlightChangesFrom(previous)

        /** then */
        val highlighted = ImageIO.read(ByteArrayInputStream(current.toPng()))
        val backgroundInChangedCell = Color(highlighted.getRGB(5, 100))
        assertEquals(255, backgroundInChangedCell.red)
        assertEquals(true, backgroundInChangedCell.green in 150..180)
        assertEquals(Color.WHITE.rgb, highlighted.getRGB(5, 50))
    }

    @Test
    fun `이미지가 아닌 값으로는 스크린샷을 만들 수 없음`() {
        /** when */
        val exception = assertFailsWith<BusinessException> {
            Screenshot.from("이미지 아님".toByteArray())
        }

        /** then */
        assertEquals(CommonError.INTERNAL_SERVER_ERROR, exception.error)
    }

    private companion object {
        const val CONTENT_HEIGHT = 40
        const val CONTENT_LEFT = 20
        const val CONTENT_RIGHT = 179
    }
}
