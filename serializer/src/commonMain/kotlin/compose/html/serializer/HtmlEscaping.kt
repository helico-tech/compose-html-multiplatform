package compose.html.serializer

fun escapeHtml(text: String): String = buildString(text.length) {
    for (ch in text) {
        when (ch) {
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '&' -> append("&amp;")
            else -> append(ch)
        }
    }
}

fun escapeAttr(text: String): String = buildString(text.length) {
    for (ch in text) {
        when (ch) {
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '&' -> append("&amp;")
            '"' -> append("&quot;")
            else -> append(ch)
        }
    }
}
