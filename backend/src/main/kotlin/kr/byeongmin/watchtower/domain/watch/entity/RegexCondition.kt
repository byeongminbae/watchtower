package kr.byeongmin.watchtower.domain.watch.entity

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue("REGEX")
class RegexCondition(
    watch: Watch,
    var regex: String
) : WatchCondition(watch)