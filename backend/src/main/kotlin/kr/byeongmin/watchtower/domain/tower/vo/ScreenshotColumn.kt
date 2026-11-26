package kr.byeongmin.watchtower.domain.tower.vo

import kr.byeongmin.watchtower.global.utils.isNull
import org.eclipse.jgit.diff.HistogramDiff
import org.eclipse.jgit.diff.Sequence
import org.eclipse.jgit.diff.SequenceComparator
import java.awt.Rectangle
import java.awt.image.Raster

class ScreenshotColumn(
    private val columnRaster: Raster
) {
    private val bytesPerPixel = columnRaster.numDataElements

    /** 이 컬럼의 로우마다 그 로우의 픽셀 바이트를 담은 목록
     *
     * row 높이는 1px 이고 원소 수는 이미지 높이 픽셀 수와 같음
     *
     * 원소 하나는 컬럼 폭(최대 32px) x 3바이트(B, G, R)이고, 원본에서 그 로우의 컬럼 부분만 복사해 담음
     * 예: 컬럼 폭 32px 이면 원소 하나는 96바이트 */
    private val rowBytes: List<ByteArray> = List(columnRaster.height) { rowIndex ->
        columnRaster.getDataElements(
            columnRaster.minX,
            rowIndex,
            columnRaster.width,
            1,
            null
        ) as ByteArray
    }

    /**
     * @return 이 컬럼에서 직전 컬럼과 비교해 새로 생기거나 바뀐 로우의 번호 목록을 반환함
     * 로우 번호는 현재 스크린샷 기준(0 이 맨 위)이고, 위에서 아래 순서로 중복 없이 담김
     * 한 가지 색으로만 된 로우(배경)는 바뀌었어도 넣지 않고, 직전에만 있고 사라진 로우도 현재에 없으므로 넣지 않음
     * 직전 컬럼이 없으면(직전 스크린샷이 더 좁음) 배경이 아닌 로우를 모두 반환함
     * 예: 직전 [H, A, F], 현재 [H, N, A, F] 이면 N 이 있는 현재 로우 번호 [1] 을 반환함
     */
    fun detectChangedRows(previous: ScreenshotColumn?): List<Int> {
        if (previous.isNull()) {
            return rowBytes.indices.filterNot { isSingleColorRow(it) }
        }

        val rowDiff = HistogramDiff().apply { setFallbackAlgorithm(null) }
            .diff(RowComparator, RowSequence(previous.rowBytes), RowSequence(rowBytes))

        return rowDiff.flatMap { edit ->
            (edit.beginB until edit.endB) // 현재 실존하는 row 를 대상으로 빨간색 칠할 예정
                .filterNot { isSingleColorRow(it) }
        }
    }

    fun cellsToHighlight(changedRows: List<Int>, cellSize: Int): List<Rectangle> {
        val cellIndices = changedRows.map { rowIndex -> rowIndex / cellSize }.distinct()

        return cellIndices.map { cellIndex ->
            val cell = Rectangle(
                /* x = */ columnRaster.minX,
                /* y = */ cellIndex * cellSize,
                /* width = */ columnRaster.width,
                /* height = */ cellSize
            )
            cell.intersection(columnRaster.bounds)
        }
    }

    /** 렌더링 문제나 의미없는 빈줄이 생겨도 그 부분을 무시하기 위한 메서드
     * 이 컬럼의 rowIndex 번째 로우(폭 최대 32px, 높이 1px)의 모든 픽셀이 그 로우의 첫 번째 픽셀과 같은 색인지 판단(배경 여부) */
    private fun isSingleColorRow(rowIndex: Int): Boolean {
        val row = rowBytes[rowIndex]
        return (bytesPerPixel until row.size).all { index ->
            row[index] == row[index % bytesPerPixel]
        }
    }
}

private class RowSequence(val rowBytes: List<ByteArray>) : Sequence() {
    override fun size(): Int {
        return rowBytes.size
    }
}

private object RowComparator : SequenceComparator<RowSequence>() {
    override fun equals(a: RowSequence, ai: Int, b: RowSequence, bi: Int): Boolean {
        return a.rowBytes[ai].contentEquals(b.rowBytes[bi])
    }

    override fun hash(seq: RowSequence, ptr: Int): Int {
        return seq.rowBytes[ptr].contentHashCode()
    }
}
