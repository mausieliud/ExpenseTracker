package com.example.expensetracker.mpesa.regex

import android.provider.Telephony
import android.util.Log
import com.example.expensetracker.mpesa.classes.MPesaTransaction
import java.util.regex.Pattern


/**
 * Regex code that extracts the details from the messages
 */
fun extractMPesaTransactions(context: android.content.Context): Pair<List<MPesaTransaction>, String> {
    val transactions = mutableListOf<MPesaTransaction>()
    val debugInfo = StringBuilder()

    // Define regex patterns for different types of M-Pesa messages

    // Pattern for money sent
    val sentMoneyPattern = Pattern.compile(
        "(?i)(?:MPESA\\s*)?(?<txnId>[A-Z0-9]+)\\s+Confirmed\\.\\s+" +
                "Ksh(?<amount>[\\d,.]+)\\s+sent\\s+to\\s+(?<recipient>\\S+(?:\\s+[^0-9]+)?)" +
                "(?:[^0-9]+)?(?<phone>\\d+)?.*" +
                "on\\s+(?<date>[^.]+)(?:[^0-9]+)(?:New balance is Ksh(?<balance>[\\d,.]+))?" +
                "(?:[^0-9]+)?(?:Transaction cost, Ksh(?<fee>[\\d,.]+))?"
    )

    // Pattern for money received - expanded to match more variations
    val receivedMoneyPattern = Pattern.compile(
        "(?i)(?:MPESA\\s*)?(?<txnId>[A-Z0-9]+)\\s+Confirmed\\.\\s*" +
                "You\\s+have\\s+received\\s+Ksh(?<amount>[\\d,.]+)\\s+from\\s+" +
                "(?<sender>[^0-9]+?)\\s+(?<phone>\\d+)\\s+on\\s+(?<date>\\d+/\\d+/\\d+)\\s+at\\s+(?<time>[^\\s]+(?:\\s+[AP]M)?)\\s+" +
                "(?:New\\s+M-PESA\\s+balance\\s+is\\s+Ksh(?<balance>[\\d,.]+))?"
    )

    // Alternate pattern for money received - some variations use different wording
    val altReceivedMoneyPattern = Pattern.compile(
        "(?i)(?:MPESA\\s*)?(?<txnId>[A-Z0-9]+)\\s+Confirmed\\.\\s*" +
                "You\\s+have\\s+received\\s+Ksh(?<amount>[\\d,.]+)\\s+from\\s+" +
                "(?<sender>[^0-9]+?)\\s+(?<phone>\\d+)\\s+on\\s+(?<datetime>[^.]+)" +
                "(?:[^0-9]+)?(?:New\\s+(?:M-PESA\\s+)?balance\\s+is\\s+Ksh(?<balance>[\\d,.]+))?"
    )

    // Pattern for payments
    val paymentPattern = Pattern.compile(
        "(?i)(?:MPESA\\s*)?(?<txnId>[A-Z0-9]+)\\s+Confirmed\\.\\s+" +
                "Ksh(?<amount>[\\d,.]+)\\s+paid\\s+to\\s+(?<recipient>[^.]+?)(?:\\.|\\son)\\s+" +
                "(?<date>[^.]+)(?:[^0-9]+)?(?:New balance is Ksh(?<balance>[\\d,.]+))?" +
                "(?:[^0-9]+)?(?:Transaction cost, Ksh(?<fee>[\\d,.]+))?"
    )

    // Pattern for withdrawals
    val withdrawalPattern = Pattern.compile(
        "(?i)(?:MPESA\\s*)?(?<txnId>[A-Z0-9]+)\\s+Confirmed\\.\\s+" +
                "Ksh(?<amount>[\\d,.]+)\\s+withdrawn\\s+from\\s+(?<agent>[^0-9]+?)\\s+" +
                "(?<agentNo>\\d+)\\s+on\\s+(?<date>[^.]+)(?:[^0-9]+)?(?:New balance is Ksh(?<balance>[\\d,.]+))?" +
                "(?:[^0-9]+)?(?:Transaction cost, Ksh(?<fee>[\\d,.]+))?"
    )

    // Pattern for deposits
    val depositPattern = Pattern.compile(
        "(?i)(?:MPESA\\s*)?(?<txnId>[A-Z0-9]+)\\s+Confirmed\\.\\s+" +
                "(?<amount>Ksh[\\d,.]+)\\s+has been deposited in your account on\\s+" +
                "(?<date>[^.]+)(?:[^0-9]+)?(?:New balance is Ksh(?<balance>[\\d,.]+))?"
    )

    try {
        // Don't filter by address to catch all possible M-Pesa messages
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(
                Telephony.Sms.Inbox.ADDRESS,
                Telephony.Sms.Inbox.BODY,
                Telephony.Sms.Inbox.DATE
            ),
            null, // No filter initially to catch all messages
            null,
            "${Telephony.Sms.Inbox.DATE} DESC"
        )

        var totalMessages = 0
        var mpesaMessages = 0

        cursor?.use {
            val addressIndex = it.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.Inbox.BODY)
            val dateIndex = it.getColumnIndex(Telephony.Sms.Inbox.DATE)

            totalMessages = cursor.count

            while (it.moveToNext()) {
                val sender = it.getString(addressIndex) ?: ""
                val body = it.getString(bodyIndex) ?: ""
                val date = it.getLong(dateIndex)

                // Check if this is likely an M-Pesa message
                if (sender.contains("MPESA", ignoreCase = true) ||
                    body.contains("MPESA", ignoreCase = true) ||
                    body.contains("confirmed", ignoreCase = true) &&
                    (body.contains("Ksh") || body.contains("received") ||
                            body.contains("sent") || body.contains("paid"))) {

                    mpesaMessages++

                    // Try to match with each pattern
                    val sentMatcher = sentMoneyPattern.matcher(body)
                    val receivedMatcher = receivedMoneyPattern.matcher(body)
                    val altReceivedMatcher = altReceivedMoneyPattern.matcher(body)
                    val paymentMatcher = paymentPattern.matcher(body)
                    val withdrawalMatcher = withdrawalPattern.matcher(body)
                    val depositMatcher = depositPattern.matcher(body)

                    when {
                        sentMatcher.find() -> {
                            transactions.add(
                                MPesaTransaction(
                                    transactionId = sentMatcher.group("txnId")?.trim() ?: "",
                                    amount = "KSh " + (sentMatcher.group("amount")?.trim() ?: ""),
                                    senderOrRecipient = sentMatcher.group("recipient")?.trim() ?: "",
                                    transactionType = "SENT",
                                    dateTime = date,
                                    balance = sentMatcher.group("balance")?.let { bal -> "KSh $bal" },
                                    fee = sentMatcher.group("fee")?.let { fee -> "KSh $fee" },
                                    rawMessage = body
                                )
                            )
                        }
                        receivedMatcher.find() -> {
                            transactions.add(
                                MPesaTransaction(
                                    transactionId = receivedMatcher.group("txnId")?.trim() ?: "",
                                    amount = "KSh " + (receivedMatcher.group("amount")?.trim() ?: ""),
                                    senderOrRecipient = receivedMatcher.group("sender")?.trim() ?: "",
                                    transactionType = "RECEIVED",
                                    dateTime = date,
                                    balance = receivedMatcher.group("balance")?.let { bal -> "KSh $bal" },
                                    rawMessage = body
                                )
                            )
                        }
                        altReceivedMatcher.find() -> {
                            transactions.add(
                                MPesaTransaction(
                                    transactionId = altReceivedMatcher.group("txnId")?.trim() ?: "",
                                    amount = altReceivedMatcher.group("amount")?.trim() ?: "",
                                    senderOrRecipient = altReceivedMatcher.group("sender")?.trim() ?: "",
                                    transactionType = "RECEIVED",
                                    dateTime = date,
                                    balance = altReceivedMatcher.group("balance")?.let { bal -> "KSh $bal" },
                                    rawMessage = body
                                )
                            )
                        }
                        paymentMatcher.find() -> {
                            transactions.add(
                                MPesaTransaction(
                                    transactionId = paymentMatcher.group("txnId")?.trim() ?: "",
                                    amount = "KSh " + (paymentMatcher.group("amount")?.trim() ?: ""),
                                    senderOrRecipient = paymentMatcher.group("recipient")?.trim() ?: "",
                                    transactionType = "PAID",
                                    dateTime = date,
                                    balance = paymentMatcher.group("balance")?.let { bal -> "KSh $bal" },
                                    fee = paymentMatcher.group("fee")?.let { fee -> "KSh $fee" },
                                    rawMessage = body
                                )
                            )
                        }
                        withdrawalMatcher.find() -> {
                            transactions.add(
                                MPesaTransaction(
                                    transactionId = withdrawalMatcher.group("txnId")?.trim() ?: "",
                                    amount = "KSh " + (withdrawalMatcher.group("amount")?.trim() ?: ""),
                                    senderOrRecipient = "Agent " + (withdrawalMatcher.group("agent")?.trim() ?: ""),
                                    transactionType = "WITHDRAW",
                                    dateTime = date,
                                    balance = withdrawalMatcher.group("balance")?.let { bal -> "KSh $bal" },
                                    fee = withdrawalMatcher.group("fee")?.let { fee -> "KSh $fee" },
                                    rawMessage = body
                                )
                            )
                        }
                        depositMatcher.find() -> {
                            transactions.add(
                                MPesaTransaction(
                                    transactionId = depositMatcher.group("txnId")?.trim() ?: "",
                                    amount = depositMatcher.group("amount")?.trim() ?: "",
                                    senderOrRecipient = "Deposit to account",
                                    transactionType = "DEPOSIT",
                                    dateTime = date,
                                    balance = depositMatcher.group("balance")?.let { bal -> "KSh $bal" },
                                    rawMessage = body
                                )
                            )
                        }
                        // If none of the patterns match but it contains MPESA, add as unknown type
                        body.contains("MPESA", ignoreCase = true) -> {
                            // This is for debug - helps understand message formats we might be missing
                            Log.d("MPesaParser", "Unmatched MPESA message: $body")
                        }
                    }
                }
            }
        }

        debugInfo.append("Total SMS: $totalMessages, M-Pesa SMS: $mpesaMessages")

    } catch (e: Exception) {
        // Handle potential exceptions
        debugInfo.append("Error: ${e.message}")
        e.printStackTrace()
    }

    return Pair(transactions, debugInfo.toString())
}