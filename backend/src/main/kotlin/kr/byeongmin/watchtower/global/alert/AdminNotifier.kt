package kr.byeongmin.watchtower.global.alert

interface AdminNotifier {
    fun sendErrorAlert(message: String, exception: Throwable)

    fun sendWarningAlert(message: String)
}
