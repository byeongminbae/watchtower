package kr.byeongmin.watchtower.global.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kr.byeongmin.watchtower.global.utils.isNull
import java.time.Duration
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [DurationMultipleOfValidator::class])
annotation class DurationMultipleOf(
    val seconds: Long,
    val message: String = "기간은 {seconds}초의 배수여야 합니다.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class DurationMultipleOfValidator : ConstraintValidator<DurationMultipleOf, Duration> {
    private var multipleSeconds: Long = 1

    override fun initialize(constraintAnnotation: DurationMultipleOf) {
        multipleSeconds = constraintAnnotation.seconds
        require(multipleSeconds > 0)
    }

    override fun isValid(value: Duration?, context: ConstraintValidatorContext): Boolean {
        return value.isNull() || (value.nano == 0 && value.seconds % multipleSeconds == 0L)
    }
}
