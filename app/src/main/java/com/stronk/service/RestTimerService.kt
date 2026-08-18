package com.stronk.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.stronk.MainActivity
import com.stronk.R
import com.stronk.ui.workout.WorkoutConstants
import com.stronk.ui.workout.WorkoutLabels
import com.stronk.ui.workout.WorkoutSession
import com.stronk.ui.workout.WorkoutSessionManager
import com.stronk.ui.workout.buildPrefill
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service trybu treningu (ADR-005): ongoing notyfikacja
 * z odliczaniem przerwy i akcją "✓ Zalicz serię" działającą z lock screena,
 * plus dźwięk + wibracja na koniec przerwy — także z wygaszonym ekranem
 * (partial wake lock na czas odliczania).
 *
 * Serwis żyje TYLKO w trakcie treningu: startuje go WorkoutViewModel przy
 * wejściu w sesję, a gdy [WorkoutSessionManager] traci sesję (zapis albo
 * porzucenie), serwis sam się zatrzymuje. Jedynym źródłem prawdy o stanie
 * jest sesja w managerze — serwis tylko ją obserwuje i przekazuje akcje.
 */
class RestTimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var restEndJob: Job? = null

    /** Koniec przerwy, na który mamy uzbrojony alarm (żeby nie dublować przy każdej emisji). */
    private var restScheduledFor: Long? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var foregroundStarted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        scope.launch {
            WorkoutSessionManager.session.collect { session ->
                if (session == null) {
                    stopSelf()
                } else {
                    if (foregroundStarted) {
                        notificationManager.notify(NOTIFICATION_ID, buildNotification(session))
                    }
                    scheduleRestEnd(session)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_COMPLETE_SET -> WorkoutSessionManager.completeCurrentSet()
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        val session = WorkoutSessionManager.session.value
        // startForeground musi pójść szybko po każdym startForegroundService.
        startInForeground(buildNotification(session))
        if (session == null) stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        restEndJob?.cancel()
        scope.cancel()
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    // ------------------------------------------------------------ rest timer

    private fun scheduleRestEnd(session: WorkoutSession) {
        val end = session.restEndsAtMillis
        if (end == null) {
            restEndJob?.cancel()
            restEndJob = null
            restScheduledFor = null
            releaseWakeLock()
            return
        }
        if (restScheduledFor == end) return
        restEndJob?.cancel()
        restScheduledFor = end
        val remaining = end - System.currentTimeMillis()
        acquireWakeLock(remaining + WorkoutConstants.WAKE_LOCK_MARGIN_MILLIS)
        restEndJob = scope.launch {
            if (remaining > 0) delay(remaining)
            signalRestEnd()
            releaseWakeLock()
            // Koniec przerwy → czyścimy pole; UI i notyfikacja przechodzą w "czas na serię".
            WorkoutSessionManager.mutate { s ->
                if (s.restEndsAtMillis == end) s.skipRest() else s
            }
        }
    }

    /** Dźwięk + wibracja na koniec przerwy (systemowe API, bez własnych assetów). */
    private fun signalRestEnd() {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(applicationContext, uri)?.play()
        }.onFailure { Log.w(TAG, "Dźwięk końca przerwy nie zagrał", it) }
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager =
                    getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(
                VibrationEffect.createWaveform(WorkoutConstants.REST_END_VIBRATION_PATTERN, -1),
            )
        }.onFailure { Log.w(TAG, "Wibracja końca przerwy nie zadziałała", it) }
    }

    private fun acquireWakeLock(timeoutMillis: Long) {
        releaseWakeLock()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
            acquire(timeoutMillis.coerceAtLeast(1_000L))
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    // ----------------------------------------------------------- notyfikacja

    private val notificationManager: NotificationManager
        get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun createChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Trwający trening",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Rest timer i odhaczanie serii z lock screena"
                setShowBadge(false)
            },
        )
    }

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        foregroundStarted = true
    }

    private fun buildNotification(session: WorkoutSession?): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_workout_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setContentIntent(openAppIntent())
        if (session == null) {
            return builder.setContentTitle("Trening").setContentText("Kończenie…").build()
        }

        val now = System.currentTimeMillis()
        val current = session.currentExercise
        val restEndsAt = session.restEndsAtMillis
        val resting = restEndsAt != null && restEndsAt > now
        when {
            session.allFinished -> builder
                .setContentTitle("Trening zrobiony")
                .setContentText("Otwórz aplikację i zapisz trening.")

            resting -> builder
                .setContentTitle("Przerwa")
                .setContentText(
                    current?.let {
                        "Następna: ${it.name} — seria ${it.nextSetNumber} z ${it.proposal.sets}"
                    } ?: "Odpocznij",
                )
                // Systemowy chronometr odlicza sam — zero odświeżania co sekundę.
                .setWhen(restEndsAt!!)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)

            else -> builder
                .setContentTitle("Trening: ${session.dayName}")
                .setContentText(
                    current?.let { "${it.name} — seria ${it.nextSetNumber} z ${it.proposal.sets}" }
                        .orEmpty(),
                )
        }
        // Wielki ✓ z lock screena — tylko gdy prefill nie wymaga wpisania ciężaru.
        if (current != null && !session.allFinished && !session.currentPrefillNeedsInput) {
            val prefill = buildPrefill(current, session.workoutId, now)
            builder.addAction(
                0,
                "✓ Zalicz serię (${WorkoutLabels.setValue(prefill)})",
                completeSetIntent(),
            )
        }
        return builder.build()
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        REQUEST_OPEN_APP,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun completeSetIntent(): PendingIntent = PendingIntent.getService(
        this,
        REQUEST_COMPLETE_SET,
        Intent(this, RestTimerService::class.java).setAction(ACTION_COMPLETE_SET),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        private const val TAG = "RestTimerService"
        private const val CHANNEL_ID = "workout"
        private const val NOTIFICATION_ID = 1
        private const val REQUEST_OPEN_APP = 1
        private const val REQUEST_COMPLETE_SET = 2
        private const val WAKE_LOCK_TAG = "stronk:rest-timer"

        const val ACTION_COMPLETE_SET = "com.stronk.action.COMPLETE_SET"
        const val ACTION_STOP = "com.stronk.action.STOP_WORKOUT"

        /** Start na czas treningu (foreground + notyfikacja) — wołane z ekranu w foreground. */
        fun start(context: Context) {
            context.startForegroundService(Intent(context, RestTimerService::class.java))
        }

        /** Stop przy zakończeniu/porzuceniu treningu. */
        fun stop(context: Context) {
            context.stopService(Intent(context, RestTimerService::class.java))
        }
    }
}
