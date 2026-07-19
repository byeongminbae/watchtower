package kr.byeongmin.watchtower.domain.watch.entity

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue("KEYWORD")
class KeywordCondition(
    watch: Watch,
    var keyword: String
) : WatchCondition(watch)