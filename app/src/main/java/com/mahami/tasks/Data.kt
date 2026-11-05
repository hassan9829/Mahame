package com.mahami.tasks

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val CATEGORIES = listOf("شخصي", "عمل", "دراسة", "تسوق", "أخرى")
val REPEATS = listOf("بدون", "يومياً", "أسبوعياً", "شهرياً")

data class Sub(val done: Boolean, val text: String)

fun encodeSubs(l: List<Sub>): String =
    l.joinToString("\n") { (if (it.done) "1|" else "0|") + it.text.replace("\n", " ") }

fun decodeSubs(s: String): List<Sub> =
    if (s.isBlank()) emptyList() else s.split("\n").map { Sub(it.startsWith("1|"), it.drop(2)) }

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val priority: Int = 1,          // 0 منخفضة، 1 متوسطة، 2 عالية
    val category: String = CATEGORIES[0],
    val dueAt: Long? = null,
    val remind: Boolean = false,
    val repeat: Int = 0,            // 0 بدون، 1 يومي، 2 أسبوعي، 3 شهري
    val subtasks: String = "",
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY done ASC, CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END, dueAt ASC, priority DESC")
    fun all(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: Long): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(t: Task): Long

    @Delete
    suspend fun delete(t: Task)

    @Query("DELETE FROM tasks WHERE done = 1")
    suspend fun clearDone()
}

@Database(entities = [Task::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): TaskDao

    companion object {
        @Volatile private var inst: AppDatabase? = null
        fun get(c: Context): AppDatabase = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, AppDatabase::class.java, "tasks.db")
                .build().also { inst = it }
        }
    }
}

class TaskViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()
    val tasks: StateFlow<List<Task>> =
        dao.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(t: Task) {
        viewModelScope.launch {
            val id = dao.upsert(t)
            Reminders.schedule(getApplication(), t.copy(id = if (t.id == 0L) id else t.id))
        }
    }

    fun toggle(t: Task) {
        viewModelScope.launch {
            val d = t.dueAt
            val n = if (!t.done && t.repeat != 0 && d != null) t.copy(dueAt = nextDue(d, t.repeat))
            else t.copy(done = !t.done)
            dao.upsert(n)
            Reminders.schedule(getApplication(), n)
        }
    }

    fun delete(t: Task) {
        viewModelScope.launch {
            dao.delete(t)
            Reminders.cancel(getApplication(), t.id)
        }
    }

    fun clearDone() {
        viewModelScope.launch { dao.clearDone() }
    }
}

fun nextDue(from: Long, repeat: Int): Long {
    val c = Calendar.getInstance().apply { timeInMillis = from }
    val now = System.currentTimeMillis()
    do {
        when (repeat) {
            1 -> c.add(Calendar.DAY_OF_YEAR, 1)
            2 -> c.add(Calendar.WEEK_OF_YEAR, 1)
            else -> c.add(Calendar.MONTH, 1)
        }
    } while (c.timeInMillis <= now)
    return c.timeInMillis
}

fun dayStart(ms: Long): Long = Calendar.getInstance().apply {
    timeInMillis = ms
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun addDays(ms: Long, n: Int): Long =
    Calendar.getInstance().apply { timeInMillis = ms; add(Calendar.DAY_OF_YEAR, n) }.timeInMillis

fun dueLabel(ms: Long): String {
    val today = dayStart(System.currentTimeMillis())
    val ar = Locale.forLanguageTag("ar")
    val time = SimpleDateFormat("h:mm a", ar).format(Date(ms))
    return when {
        ms in today until addDays(today, 1) -> "اليوم $time"
        ms in addDays(today, 1) until addDays(today, 2) -> "غداً $time"
        ms in addDays(today, -1) until today -> "أمس $time"
        else -> SimpleDateFormat("d MMM yyyy", ar).format(Date(ms)) + " $time"
    }
}
