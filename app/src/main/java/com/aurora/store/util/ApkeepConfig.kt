package com.aurora.store.util

import java.io.InputStream

/** Bounded local import. Never logs or persists source bytes. */
internal object ApkeepConfig {
    private const val LIMIT = 65536
    class Credentials(val email: String, val token: String)

    fun read(input: InputStream): Credentials {
        val bytes = ByteArray(LIMIT + 1)
        try {
            var size = 0
            while (size < bytes.size) {
                val count = input.read(bytes, size, bytes.size - size)
                if (count < 0) break
                if (count == 0) error("Empty read")
                size += count
            }
            require(size <= LIMIT)
            return parse(String(bytes, 0, size, Charsets.UTF_8))
        } finally {
            bytes.fill(0)
        }
    }

    fun parse(text: String): Credentials {
        require(text.length <= LIMIT)
        var google = false
        var seenGoogle = false
        var email: String? = null
        var token: String? = null
        for (raw in text.removePrefix("\uFEFF").lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) continue
            if (line.startsWith("[") && line.endsWith("]")) {
                google = line == "[google]"
                if (google) {
                    require(!seenGoogle)
                    seenGoogle = true
                }
                continue
            }
            if (!google) continue
            val key = line.substringBefore('=').trim()
            if (key != "email" && key != "aas_token") continue
            require('=' in line)
            val value = line.substringAfter('=').trim()
            require(value.isNotEmpty() && value.none { it.isWhitespace() || it.isISOControl() })
            when (key) {
                "email" -> { require(email == null); email = value }
                "aas_token" -> { require(token == null); token = value }
            }
        }
        val e = requireNotNull(email)
        require(e.count { it == '@' } == 1 && e.substringBefore('@').isNotEmpty() && e.substringAfter('@').isNotEmpty())
        return Credentials(e, requireNotNull(token))
    }
}
