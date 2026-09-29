package com.asahioo.moodly.data.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.asahioo.moodly.data.model.HealthDay
import com.asahioo.moodly.data.repository.MoodRepository
import com.asahioo.moodly.domain.DateProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period

sealed interface HealthStatus {
    /** Android < 9 o sin proveedor: la sección se muestra deshabilitada. */
    data object Unavailable : HealthStatus
    data object NeedsUpdate : HealthStatus
    data object Disconnected : HealthStatus
    /** Permisos parciales: se lee solo lo concedido. */
    data class Connected(val sleep: Boolean, val steps: Boolean) : HealthStatus
}

/**
 * Lee pasos y sueño de Health Connect y los guarda por día. Solo lectura y solo en primer plano;
 * los permisos viven en Health Connect (el usuario puede revocarlos desde ahí), así que no hay
 * ajuste propio: [status] se recalcula en cada [refresh].
 */
class HealthConnect(
    private val context: Context,
    private val repository: MoodRepository,
    private val dates: DateProvider,
) {
    private val _status = MutableStateFlow<HealthStatus>(HealthStatus.Unavailable)
    val status: StateFlow<HealthStatus> = _status

    private val mutex = Mutex()

    private fun client(): HealthConnectClient? =
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }

    suspend fun refresh() {
        _status.value = when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> {
                val granted = safely { client()?.permissionController?.getGrantedPermissions() }.orEmpty()
                val sleep = READ_SLEEP in granted
                val steps = READ_STEPS in granted
                if (sleep || steps) HealthStatus.Connected(sleep, steps) else HealthStatus.Disconnected
            }
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthStatus.NeedsUpdate
            else -> HealthStatus.Unavailable
        }
    }

    /** Relee los últimos [days] días (incluido hoy). Un fallo solo significa "no se importó ahora". */
    suspend fun sync(days: Int = 7): Unit = mutex.withLock {
        val connected = _status.value as? HealthStatus.Connected ?: return
        val hc = client() ?: return
        val today = dates.today()
        val from = today.minusDays(days - 1L)
        val steps = if (connected.steps) safely { readSteps(hc, from, today) }.orEmpty() else emptyMap()
        val sleep = if (connected.sleep) {
            (0 until days).map { from.plusDays(it.toLong()) }
                .mapNotNull { date -> safely { readSleep(hc, date) }?.let { date to it } }
                .toMap()
        } else {
            emptyMap()
        }
        val merged = (steps.keys + sleep.keys).associateWith { HealthDay(sleep[it], steps[it]) }
        safely { repository.importHealth(merged) }
        Unit
    }

    /** Revoca los permisos. Lo ya importado se queda en los registros. */
    suspend fun disconnect() {
        safely { client()?.permissionController?.revokeAllPermissions() }
        refresh()
    }

    /** Una sola llamada para todo el rango; Health Connect ya deduplica entre apps. */
    private suspend fun readSteps(hc: HealthConnectClient, from: LocalDate, to: LocalDate): Map<LocalDate, Int> =
        hc.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(from.atStartOfDay(), to.plusDays(1).atStartOfDay()),
                timeRangeSlicer = Period.ofDays(1),
            )
        ).mapNotNull { group ->
            group.result[StepsRecord.COUNT_TOTAL]?.let { group.startTime.toLocalDate() to it.toInt() }
        }.toMap()

    /**
     * Sueño de la noche anterior a [date]: el agregado resuelve el solapamiento teléfono + reloj
     * según la prioridad de fuentes que el usuario eligió en Health Connect.
     */
    private suspend fun readSleep(hc: HealthConnectClient, date: LocalDate): Int? =
        hc.aggregate(
            AggregateRequest(
                metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(
                    date.minusDays(1).atTime(SLEEP_WINDOW_START),
                    date.atTime(SLEEP_WINDOW_END),
                ),
            )
        )[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.toMinutes()?.toInt()

    /** Permiso revocado a medio camino, cuota agotada o proveedor caído: nada que perder, se reintenta al volver. */
    private suspend fun <T> safely(block: suspend () -> T): T? = try {
        block()
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.w(TAG, "Health Connect", e)
        null
    }

    companion object {
        private const val TAG = "HealthConnect"
        val READ_SLEEP = HealthPermission.getReadPermission(SleepSessionRecord::class)
        val READ_STEPS = HealthPermission.getReadPermission(StepsRecord::class)
        val PERMISSIONS = setOf(READ_SLEEP, READ_STEPS)

        /** Días a rellenar justo al conectar (Health Connect deja leer ~30 días previos al permiso). */
        const val BACKFILL_DAYS = 30

        // ponytail: ventana fija 18:00–14:00; una siesta después de las 14:00 no cuenta. Ajustar aquí si molesta.
        private val SLEEP_WINDOW_START: LocalTime = LocalTime.of(18, 0)
        private val SLEEP_WINDOW_END: LocalTime = LocalTime.of(14, 0)

        /** Abre la ficha de Health Connect en Play Store para instalar/actualizar el proveedor. */
        const val PLAY_STORE_URI =
            "market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"
    }
}
