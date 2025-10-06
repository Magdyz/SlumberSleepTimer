package com.app.slumber.service

import android.app.Notification
import android.app.PendingIntent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

class SlumberNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        INSTANCE.set(this)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        INSTANCE.compareAndSet(this, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        INSTANCE.compareAndSet(this, null)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val n = sbn.notification ?: return
        if (n.category == Notification.CATEGORY_TRANSPORT) {
            logNotificationActions(n)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) { }

    private fun logNotificationActions(n: Notification) {
        val actions = n.actions ?: return
        if (actions.isEmpty()) return
    }

    private fun semanticLabel(a: Notification.Action): String {
        if (Build.VERSION.SDK_INT < 28) return ""
        return when (a.semanticAction) {
            12 -> "PAUSE"
            13 -> "PLAY"
            14 -> "STOP"
            3  -> "DELETE"
            4  -> "ARCHIVE"
            else -> a.semanticAction.toString()
        }
    }

    companion object {
        private val INSTANCE = AtomicReference<SlumberNotificationListener?>(null)

        private object ActionCompat {
            const val SEMANTIC_ACTION_PAUSE = 12
            const val SEMANTIC_ACTION_PLAY = 13
            const val SEMANTIC_ACTION_STOP = 14
        }

        fun performPauseOrStopOnActiveMediaNotifications(
            preferStop: Boolean = true,
            targetPackages: Set<String>? = null
        ): Boolean {
            val svc = INSTANCE.get() ?: return false
            val active = try { svc.activeNotifications?.toList().orEmpty() } catch (_: Exception) { emptyList() }
            if (active.isEmpty()) return false

            var didFire = false
            active.forEach { sbn ->
                val n = sbn.notification ?: return@forEach
                if (n.category != Notification.CATEGORY_TRANSPORT) return@forEach
                if (targetPackages != null && sbn.packageName !in targetPackages) return@forEach

                val actions = n.actions ?: return@forEach
                if (actions.isEmpty()) return@forEach

                if (Build.VERSION.SDK_INT >= 28) {
                    if (preferStop) {
                        actions.firstOrNull { it.semanticAction == ActionCompat.SEMANTIC_ACTION_STOP }?.let {
                            didFire = didFire or it.safeSend()
                            return@forEach
                        }
                    }
                    actions.firstOrNull { it.semanticAction == ActionCompat.SEMANTIC_ACTION_PAUSE }?.let {
                        didFire = didFire or it.safeSend()
                        return@forEach
                    }
                }

                val matcher: (CharSequence?) -> Boolean = { title ->
                    val t = title?.toString()?.lowercase(Locale.ROOT)
                    if (t == null) false else {
                        t.contains("pause") || t.contains("stop") ||
                                t.contains("إيقاف") || t.contains("ايقاف") ||
                                t.contains("إيقاف مؤقت") || t.contains("إيقاف التشغيل")
                    }
                }

                val target = if (preferStop) {
                    actions.firstOrNull { matcher(it.title) && it.title?.toString()?.contains("stop", true) == true }
                        ?: actions.firstOrNull { matcher(it.title) }
                } else {
                    actions.firstOrNull { matcher(it.title) }
                }

                if (target != null) {
                    didFire = didFire or target.safeSend()
                }
            }
            return didFire
        }

        private fun Notification.Action.safeSend(): Boolean {
            return try {
                this.actionIntent?.send()
                true
            } catch (_: PendingIntent.CanceledException) {
                false
            } catch (_: Exception) {
                false
            }
        }
    }
}
