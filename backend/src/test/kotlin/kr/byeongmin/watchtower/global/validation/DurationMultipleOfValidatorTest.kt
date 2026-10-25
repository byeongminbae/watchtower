package kr.byeongmin.watchtower.global.validation

import jakarta.validation.ConstraintValidatorContext
import java.time.Duration
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock

class DurationMultipleOfValidatorTest {
    private val context = mock<ConstraintValidatorContext>()
    private val validator = DurationMultipleOfValidator().apply { initialize(DurationMultipleOf(seconds = 300)) }

    @Test
    fun `배수 검사에서는 필수값과 최소값을 제한하지 않음`() {
        /** given */
        val durations = listOf(
            null,
            Duration.ZERO,
            Duration.ofSeconds(300),
            Duration.ofSeconds(-600),
            Duration.ofSeconds(Long.MAX_VALUE - 7)
        )

        durations.forEach { duration ->
            /** when */
            val result = validator.isValid(duration, context)

            /** then */
            assertTrue(result, duration.toString())
        }
    }

    @Test
    fun `지정한 초 단위의 배수가 아니거나 초 미만 값이 있으면 허용하지 않음`() {
        /** given */
        val durations = listOf(
            Duration.ofSeconds(301),
            Duration.ofSeconds(-301),
            Duration.ofSeconds(300, 1),
            Duration.ofSeconds(-300, -1)
        )

        durations.forEach { duration ->
            /** when */
            val result = validator.isValid(duration, context)

            /** then */
            assertFalse(result, duration.toString())
        }
    }

    @ParameterizedTest
    @ValueSource(longs = [0, -1])
    fun `배수 검사의 기준 간격은 0보다 커야 함`(seconds: Long) {
        /** when */
        val result = runCatching {
            validator.initialize(DurationMultipleOf(seconds = seconds))
        }

        /** then */
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
    }
}
