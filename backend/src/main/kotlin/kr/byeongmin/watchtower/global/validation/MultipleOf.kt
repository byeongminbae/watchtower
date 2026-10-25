package kr.byeongmin.watchtower.global.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kr.byeongmin.watchtower.global.utils.isNull
import kotlin.reflect.KClass

/** 숫자가 지정한 값의 배수인지 검사 */
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.ANNOTATION_CLASS
)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [MultipleOfValidator::class])
annotation class MultipleOf(
    val value: Long,
    val message: String = "값은 {value}의 배수여야 합니다.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class MultipleOfValidator : ConstraintValidator<MultipleOf, Number> {
    private var divisor: Long = 1

    override fun initialize(constraintAnnotation: MultipleOf) {
        divisor = constraintAnnotation.value
    }

    override fun isValid(value: Number?, context: ConstraintValidatorContext): Boolean {
        return value.isNull() || divisor > 0 && value.toLong() % divisor == 0L
    }
}
