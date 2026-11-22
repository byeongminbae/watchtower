package kr.byeongmin.watchtower.domain.notification.repository

import kr.byeongmin.watchtower.domain.notification.entity.Notification
import kr.byeongmin.watchtower.global.repository.BaseRepository

interface NotificationRepository : BaseRepository<Notification, Long>
