package com.example.ui.theme

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

enum class AppLang(val code: String, val toggleLabel: String) {
    BN("bn", "EN"),
    EN("en", "বাং");

    companion object {
        fun fromCode(code: String?): AppLang = if (code == "en") EN else BN
    }
}

fun tr(lang: AppLang, bn: String, en: String): String = if (lang == AppLang.BN) bn else en

object BnFormat {
    private val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun toBnDigits(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s) {
            if (ch in '0'..'9') sb.append(bnDigits[ch - '0']) else sb.append(ch)
        }
        return sb.toString()
    }

    fun toAsciiDigits(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s) {
            val idx = bnDigits.indexOf(ch)
            if (idx >= 0) sb.append(('0'.code + idx).toChar()) else sb.append(ch)
        }
        return sb.toString()
    }

    fun num(value: Double, decimals: Int = 0, lang: AppLang = AppLang.BN): String {
        val formatted = if (decimals <= 0) {
            NumberFormat.getIntegerInstance(Locale.US).format(value.roundToInt())
        } else {
            String.format(Locale.US, "%,.${decimals}f", value)
        }
        return if (lang == AppLang.BN) toBnDigits(formatted) else formatted
    }

    fun num(value: Double, lang: AppLang): String = num(value, 0, lang)

    fun num(value: Int, lang: AppLang = AppLang.BN): String = num(value.toDouble(), 0, lang)

    fun money(amount: Number, lang: AppLang = AppLang.BN): String {
        val n = amount.toDouble()
        val sign = if (n < 0) "-" else ""
        val absNum = num(kotlin.math.abs(n), 0, lang)
        return "$sign$absNum " + if (lang == AppLang.BN) "৳" else "BDT"
    }

    fun signedMoney(amount: Number, lang: AppLang = AppLang.BN): String {
        val n = amount.toDouble()
        val sign = if (n > 0) "+" else if (n < 0) "-" else ""
        val absNum = num(kotlin.math.abs(n), 0, lang)
        return "$sign$absNum " + if (lang == AppLang.BN) "৳" else "BDT"
    }
}
