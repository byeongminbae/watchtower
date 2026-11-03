package kr.byeongmin.watchtower.external.slack.client

/**
 * Slack 으로 내보내는 문장에서 인증 값과 웹훅 URL 을 가림
 *
 * 정해 둔 형식만 가리므로 새로운 민감 정보를 알림에 담게 되면 여기에 규칙을 추가해야 함
 */
object SensitiveValueMasker {
    private const val MASK = "***"
    private const val SENSITIVE_KEYS = "code|state|access_token|refresh_token|client_secret|token"

    private val keyValuePattern = Regex("""(?<![A-Za-z0-9_])($SENSITIVE_KEYS)=[^,&\s]+""")
    private val jsonFieldPattern = Regex(""""($SENSITIVE_KEYS)"\s*:\s*"[^"]*"""")
    private val bearerTokenPattern = Regex("""(?i)(bearer\s+)[A-Za-z0-9._~+/=-]+""")
    private val webhookUrlPattern = Regex("""https://(hooks\.slack\.com|(discord|discordapp)\.com/api/webhooks)/\S+""")

    fun mask(text: String): String {
        return text
            .replace(keyValuePattern) { "${it.groupValues[1]}=$MASK" }
            .replace(jsonFieldPattern) { "\"${it.groupValues[1]}\":\"$MASK\"" }
            .replace(bearerTokenPattern) { "${it.groupValues[1]}$MASK" }
            .replace(webhookUrlPattern) { "https://${it.groupValues[1]}/$MASK" }
    }
}
