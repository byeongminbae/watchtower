package kr.byeongmin.watchtower.domain.tower.vo

import com.twelvemonkeys.image.ImageUtil
import kr.byeongmin.watchtower.global.error.CommonError
import kr.byeongmin.watchtower.global.exception.BusinessException
import java.awt.Color
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class Screenshot private constructor(
    private val image: BufferedImage
) {
    fun highlightChangesFrom(previous: Screenshot) {
        val graphics = image.createGraphics()
        graphics.color = TRANSLUCENT_RED

        findChangedCellsComparedTo(previous).forEach { graphics.fill(it) }
        graphics.dispose()
    }

    fun toPng(): ByteArray {
        val png = ByteArrayOutputStream()
        ImageIO.write(image, PNG_FORMAT, png)
        return png.toByteArray()
    }

    private fun findChangedCellsComparedTo(previous: Screenshot): List<Rectangle> {
        val previousColumns = previous.splitToColumns()

        return splitToColumns().flatMapIndexed { index, column ->
            val changedRows = column.detectChangedRows(previousColumns.getOrNull(index))
            column.cellsToHighlight(changedRows, CELL_SIZE)
        }
    }

    private fun splitToColumns(): List<ScreenshotColumn> {
        return (0 until image.width step CELL_SIZE).map { columnStartX ->
            val columnWidth = minOf(CELL_SIZE, image.width - columnStartX)
            ScreenshotColumn(
                image.raster.createChild(
                    columnStartX,
                    0,
                    columnWidth,
                    image.height,
                    columnStartX,
                    0,
                    null
                )
            )
        }
    }

    companion object {
        private const val PNG_FORMAT = "png"
        private const val CELL_SIZE = 32
        private const val HIGHLIGHT_OPACITY = 0.35
        private val TRANSLUCENT_RED = Color(1f, 0f, 0f, HIGHLIGHT_OPACITY.toFloat())

        fun from(png: ByteArray): Screenshot {
            val image = ImageIO.read(ByteArrayInputStream(png))
                ?: throw BusinessException(CommonError.INTERNAL_SERVER_ERROR)

            // PNG 는 저장 방식에 따라 ARGB, int RGB, 팔레트 등 서로 다른 형식으로 읽힘.
            // 형식이 다르면 같은 화면이어도 픽셀 바이트가 달라 전부 바뀐 것으로 잡히고, 로우를 바이트 배열로 꺼낼 수도 없어서
            // 비교 전에 모든 스크린샷을 같은 형식으로 맞춤
            return Screenshot(ImageUtil.toBuffered(image, BufferedImage.TYPE_3BYTE_BGR))
        }
    }
}
