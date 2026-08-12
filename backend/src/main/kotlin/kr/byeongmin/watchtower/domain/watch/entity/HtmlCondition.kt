package kr.byeongmin.watchtower.domain.watch.entity

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue("HTML")
class HtmlCondition private constructor(
    watch: Watch,
) : WatchCondition(watch)