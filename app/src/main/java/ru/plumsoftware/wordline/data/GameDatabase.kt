package ru.plumsoftware.wordline.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "profile")
data class ProfileRow(@PrimaryKey val id: Int = 1, val coins: Long = 200, val campaignSessionId: String? = null)
@Entity(tableName = "sessions")
data class SessionRow(@PrimaryKey val id: String, val levelId: Int, val replay: Boolean, val json: String)
@Entity(tableName = "completions")
data class CompletionRow(@PrimaryKey val levelId: Int, val bestStars: Int, val completedAt: Long)
@Entity(tableName = "ledger")
data class LedgerRow(@PrimaryKey val operationId: String, val reason: String, val sourceId: String,
                     val amount: Long, val balanceAfter: Long, val createdAt: Long)
@Entity(tableName = "world")
data class WorldRow(@PrimaryKey val id: Int = 1, val json: String)

@Dao
interface GameDao {
    @Query("SELECT * FROM world WHERE id = 1") fun observeWorld(): Flow<WorldRow?>
    @Query("SELECT * FROM world WHERE id = 1") suspend fun world(): WorldRow?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(row: WorldRow)
    @Query("SELECT * FROM sessions") suspend fun sessions(): List<SessionRow>
    @Query("SELECT * FROM completions ORDER BY levelId") suspend fun completions(): List<CompletionRow>
    @Query("SELECT * FROM ledger ORDER BY createdAt, operationId") suspend fun ledger(): List<LedgerRow>
    @Query("DELETE FROM sessions") suspend fun clearSessions()
    @Query("DELETE FROM completions") suspend fun clearCompletions()
    @Query("DELETE FROM ledger") suspend fun clearLedger()
    @Query("SELECT * FROM profile WHERE id = 1") fun observeProfile(): Flow<ProfileRow?>
    @Query("SELECT * FROM completions ORDER BY levelId") fun observeCompletions(): Flow<List<CompletionRow>>
    @Query("SELECT * FROM sessions") fun observeSessions(): Flow<List<SessionRow>>
    @Query("SELECT * FROM profile WHERE id = 1") suspend fun profile(): ProfileRow?
    @Query("SELECT * FROM sessions WHERE id = :id") suspend fun session(id: String): SessionRow?
    @Query("SELECT * FROM completions WHERE levelId = :id") suspend fun completion(id: Int): CompletionRow?
    @Query("SELECT operationId FROM ledger WHERE operationId = :id") suspend fun operation(id: String): String?
    @Query("SELECT * FROM ledger WHERE operationId = :id") suspend fun ledgerEntry(id: String): LedgerRow?
    @Query("SELECT COALESCE(SUM(amount), 0) FROM ledger WHERE sourceId = :sessionId AND reason IN ('bonus-chest', 'bonus-complete')")
    suspend fun sessionBonusReward(sessionId: String): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(row: ProfileRow)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(row: SessionRow)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(row: CompletionRow)
    @Insert suspend fun record(row: LedgerRow)
}

@Database(entities = [ProfileRow::class, SessionRow::class, CompletionRow::class, LedgerRow::class, WorldRow::class], version = 2, exportSchema = true)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    companion object {
        val MIGRATION_1_2=object : Migration(1,2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `world` (`id` INTEGER NOT NULL, `json` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }
    }
}
