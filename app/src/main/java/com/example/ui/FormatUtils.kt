package com.example.ui

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object FormatUtils {
    private val thbDecimalFormat = DecimalFormat("#,##0.00")
    private val thbCompactFormat = DecimalFormat("#,##0")
    private val qtyIntFormat = DecimalFormat("#,##0")
    private val qtyFracFormat = DecimalFormat("#,##0.######")

    fun formatThb(amount: Double, showSign: Boolean = false): String {
        val sign = when {
            !showSign -> if (amount < 0) "-" else ""
            amount > 0.004 -> "+"
            amount < -0.004 -> "-"
            else -> ""
        }
        return "${sign}฿${thbDecimalFormat.format(abs(amount))}"
    }

    fun formatThbCompact(amount: Double): String {
        val sign = if (amount < 0) "-" else ""
        val a = abs(amount)
        return when {
            a >= 1_000_000 -> "${sign}฿${DecimalFormat("#,##0.##").format(a / 1_000_000)}M"
            a >= 10_000 -> "${sign}฿${thbCompactFormat.format(a)}"
            else -> "${sign}฿${thbDecimalFormat.format(a)}"
        }
    }

    fun formatOriginalCurrency(amount: Double, currency: String): String {
        val prefix = if (currency == "USD") "$" else "฿"
        val formatted = if (abs(amount) < 1.0 && amount != 0.0) {
            DecimalFormat("#,##0.####").format(abs(amount))
        } else {
            thbDecimalFormat.format(abs(amount))
        }
        val sign = if (amount < 0) "-" else ""
        return "$sign$prefix$formatted"
    }

    fun formatPercent(percent: Double, showSign: Boolean = false, decimals: Int = 2): String {
        val pattern = if (decimals == 1) "#,##0.0" else "#,##0.00"
        val fmt = DecimalFormat(pattern)
        val sign = when {
            !showSign -> if (percent < 0) "-" else ""
            percent > 0.004 -> "+"
            percent < -0.004 -> "-"
            else -> ""
        }
        return "$sign${fmt.format(abs(percent))}%"
    }

    fun formatQuantity(qty: Double): String {
        return if (abs(qty - qty.toLong().toDouble()) < 1e-6) {
            qtyIntFormat.format(qty.toLong())
        } else {
            qtyFracFormat.format(qty)
        }
    }

    fun formatUpdatedTime(millis: Long?): String {
        if (millis == null) return "รออัปเดตราคา"
        val diffSec = (System.currentTimeMillis() - millis) / 1000
        return when {
            diffSec < 15 -> "อัปเดตเมื่อสักครู่"
            diffSec < 60 -> "อัปเดต $diffSec วินาทีที่แล้ว"
            diffSec < 3600 -> "อัปเดต ${diffSec / 60} นาทีที่แล้ว"
            else -> {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                "อัปเดตเวลา ${sdf.format(Date(millis))}"
            }
        }
    }
}
