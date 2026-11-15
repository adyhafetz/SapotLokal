package com.sapotlokal.data.repository

import com.google.firebase.database.FirebaseDatabase
import com.sapotlokal.AppConfig
import com.sapotlokal.data.model.Game
import com.sapotlokal.data.model.GameType
import com.sapotlokal.data.model.PointSource
import java.time.LocalDate
import java.time.ZoneId

/**
 * GamesRepository — the ONLY place game plays are read or written.
 * Daily limits are computed from the live database, never from data stored on the phone (Rule 43).
 * Every play that earns points writes the /games row AND the /points row in ONE atomic update (Rule 9).
 */
object GamesRepository {

    private val db = FirebaseDatabase.getInstance(AppConfig.DATABASE_URL).reference

    fun todayKey(): String =
        LocalDate.now(ZoneId.of(AppConfig.GAMES_RESET_TIME_ZONE)).toString()

    data class GameStatus(
        val spinsToday: Int,
        val spinAttemptsTotal: Int,
        val memoryPointsToday: Int,
        val memoryAttemptsTotal: Int
    ) {
        val canSpinToday: Boolean get() = spinsToday < AppConfig.SPIN_WHEEL_DAILY_LIMIT
        val memoryRoomToday: Int
            get() = (AppConfig.MEMORY_MATCH_DAILY_CAP_POINTS - memoryPointsToday).coerceAtLeast(0)
    }

    /** One read gives everything the UI needs. onResult(null) = could not read (offline / permission). */
    fun fetchStatus(userID: String, onResult: (GameStatus?) -> Unit) {
        // Mirror the exact query pattern PointRepository.fetchAllPointsForUser() uses.
        db.child("games").orderByChild("userID").equalTo(userID).get()
            .addOnSuccessListener { snap ->
                val today = todayKey()
                var spinsToday = 0; var spinTotal = 0
                var memPointsToday = 0; var memTotal = 0
                for (child in snap.children) {
                    val g = child.getValue(Game::class.java) ?: continue
                    when (g.gameType) {
                        GameType.SPIN_WHEEL -> {
                            spinTotal++
                            if (g.gameDay == today) spinsToday++
                        }
                        GameType.MEMORY_MATCH -> {
                            memTotal++
                            if (g.gameDay == today) memPointsToday += g.gamePointsRewarded
                        }
                    }
                }
                onResult(GameStatus(spinsToday, spinTotal, memPointsToday, memTotal))
            }
            .addOnFailureListener { onResult(null) }
    }

    /** onResult(success, pointsActuallyEarned) — earned already includes the tier multiplier. */
    fun recordSpin(userID: String, pointsWon: Int, attemptNumber: Int, onResult: (Boolean, Int) -> Unit) {
        PointRepository.awardMiniGamePoints(userID, pointsWon, GameType.SPIN_WHEEL, attemptNumber, onResult = onResult)
    }

    /**
     * [roomToday] is the remaining daily allowance in FINAL (post-multiplier) points, so the
     * cap can never be exceeded even for high tiers.
     */
    fun recordMemoryWin(
        userID: String,
        basePoints: Int,
        roomToday: Int,
        attemptNumber: Int,
        onResult: (Boolean, Int) -> Unit
    ) {
        if (roomToday <= 0) {
            // Cap reached: still log the play (0 points) but award nothing.
            PointRepository.awardMiniGamePoints(userID, 0, GameType.MEMORY_MATCH, attemptNumber, onResult = onResult)
            return
        }
        PointRepository.awardMiniGamePoints(
            userID, basePoints, GameType.MEMORY_MATCH, attemptNumber,
            maxEarnedPoints = roomToday, onResult = onResult
        )
    }
}

