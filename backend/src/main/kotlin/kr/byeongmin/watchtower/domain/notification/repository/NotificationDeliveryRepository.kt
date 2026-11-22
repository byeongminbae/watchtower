package kr.byeongmin.watchtower.domain.notification.repository

import kr.byeongmin.watchtower.domain.notification.entity.NotificationDelivery
import kr.byeongmin.watchtower.global.repository.BaseRepository

interface NotificationDeliveryRepository : BaseRepository<NotificationDelivery, Long>
