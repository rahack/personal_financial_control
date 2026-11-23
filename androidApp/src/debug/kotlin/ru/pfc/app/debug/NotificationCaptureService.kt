package ru.pfc.app.debug

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONObject
import java.io.File

/**
 * Пишет уведомления банков в личный файл приложения, по строке JSON на уведомление.
 * Из этих записей готовятся обезличенные примеры для правил разбора (KTD16).
 * Служба есть только в сборке debug.
 */
class NotificationCaptureService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!isBankPackage(sbn.packageName)) return
        val notification = sbn.notification
        val extras = notification.extras
        val record = JSONObject()
            .put("package", sbn.packageName)
            .put("key", sbn.key)
            .put("postTime", sbn.postTime)
            .put("groupSummary", notification.flags and Notification.FLAG_GROUP_SUMMARY != 0)
            .put("title", extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
            .put("text", extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())
            .put("bigText", extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString())
            .put("subText", extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString())
            .put("textLines", extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString("\n"))
        captureFile(this).apply { parentFile?.mkdirs() }.appendText(record.toString() + "\n")
    }

    // Прошивки с жёсткой экономией батареи отключают службу; без переподключения записи пропадут.
    override fun onListenerDisconnected() {
        requestRebind(ComponentName(this, NotificationCaptureService::class.java))
    }
}

// noBackupFilesDir: сырые тексты не должны попасть в системный бэкап и перенос на новый телефон.
internal fun captureFile(context: Context): File = File(context.noBackupFilesDir, "captures/notifications.jsonl")

private val bankPackages = setOf(
    "com.idamob.tinkoff.android", // Т-Банк
    "ru.ozon.fintech.finance", // Озон Банк
)

// Имя пакета ВТБ не подтверждено, поэтому ВТБ узнаётся по части имени.
// Поле package в записях покажет настоящее имя.
private fun isBankPackage(packageName: String): Boolean =
    packageName in bankPackages || "vtb" in packageName.lowercase()
