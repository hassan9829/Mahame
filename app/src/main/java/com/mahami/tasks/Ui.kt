@file:OptIn(ExperimentalMaterial3Api::class)

package com.mahami.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Calendar

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0F766E), onPrimary = Color.White,
    primaryContainer = Color(0xFFD5F2EE), onPrimaryContainer = Color(0xFF053B36),
    secondaryContainer = Color(0xFFD5F2EE), onSecondaryContainer = Color(0xFF053B36),
    background = Color(0xFFF4F6F5), surface = Color.White,
    onSurfaceVariant = Color(0xFF5F6B69), outlineVariant = Color(0xFFDDE3E1)
)
private val DarkScheme = darkColorScheme(
    primary = Color(0xFF5EC4B8), onPrimary = Color(0xFF00302B),
    primaryContainer = Color(0xFF124A44), onPrimaryContainer = Color(0xFFD5F2EE),
    secondaryContainer = Color(0xFF124A44), onSecondaryContainer = Color(0xFFD5F2EE),
    background = Color(0xFF0D1211), surface = Color(0xFF151B1A),
    onSurfaceVariant = Color(0xFF9AA8A5), outlineVariant = Color(0xFF26302E)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme,
        content = content
    )
}

fun priorityColor(p: Int): Color = when (p) {
    2 -> Color(0xFFE5484D)
    1 -> Color(0xFFF5A524)
    else -> Color(0xFF30A46C)
}

private val TABS = listOf("النشطة", "اليوم", "القادمة", "المتأخرة", "المنجزة")

@Composable
fun TaskListScreen(
    tasks: List<Task>,
    onOpen: (Task) -> Unit,
    onNew: () -> Unit,
    onToggle: (Task) -> Unit,
    onClearDone: () -> Unit
) {
    var q by remember { mutableStateOf("") }
    var tab by remember { mutableIntStateOf(0) }
    var cat by remember { mutableStateOf<String?>(null) }

    val now = System.currentTimeMillis()
    val dayStart = dayStart(now)
    val dayEnd = addDays(dayStart, 1)
    val shown = tasks.filter { t ->
        val d = t.dueAt
        (q.isBlank() || t.title.contains(q, true) || t.notes.contains(q, true)) &&
            (cat == null || t.category == cat) &&
            when (tab) {
                0 -> !t.done
                1 -> !t.done && d != null && d >= dayStart && d < dayEnd
                2 -> !t.done && d != null && d >= dayEnd
                3 -> !t.done && d != null && d < now
                else -> t.done
            }
    }
    val doneCount = tasks.count { it.done }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("مهمة جديدة") }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)) {
                Text("مهامي", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "أنجزت $doneCount من ${tasks.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { if (tasks.isEmpty()) 0f else doneCount / tasks.size.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                )
            }
            OutlinedTextField(
                value = q,
                onValueChange = { q = it },
                placeholder = { Text("ابحث في المهام") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(TABS) { i, s ->
                    FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(s) })
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(selected = cat == null, onClick = { cat = null }, label = { Text("كل التصنيفات") })
                }
                items(CATEGORIES) { c ->
                    FilterChip(
                        selected = cat == c,
                        onClick = { cat = if (cat == c) null else c },
                        label = { Text(c) }
                    )
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (shown.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 56.dp), contentAlignment = Alignment.Center) {
                            Text(
                                if (tasks.isEmpty()) "لا توجد مهام بعد.\nاضغط «مهمة جديدة» لإضافة أول مهمة."
                                else "لا توجد مهام في هذا العرض.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                items(shown, key = { it.id }) { t ->
                    TaskCard(t, onOpen = { onOpen(t) }, onToggle = { onToggle(t) })
                }
                if (tab == 4 && shown.isNotEmpty()) {
                    item { TextButton(onClick = onClearDone) { Text("حذف كل المهام المنجزة") } }
                }
            }
        }
    }
}

@Composable
fun TaskCard(t: Task, onOpen: () -> Unit, onToggle: () -> Unit) {
    val d = t.dueAt
    val overdue = !t.done && d != null && d < System.currentTimeMillis()
    val pc = priorityColor(t.priority)
    val subs = remember(t.subtasks) { decodeSubs(t.subtasks) }
    val meta = listOfNotNull(
        d?.let { (if (t.remind) "🔔 " else "") + dueLabel(it) },
        t.category,
        if (subs.isNotEmpty()) "☑ ${subs.count { it.done }}/${subs.size}" else null,
        if (t.repeat != 0) "↻ " + REPEATS[t.repeat] else null
    )
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(pc))
            Box(
                Modifier.padding(start = 4.dp).size(44.dp).clip(CircleShape).clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(26.dp).clip(CircleShape)
                        .background(if (t.done) pc else Color.Transparent)
                        .border(2.dp, pc, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (t.done) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Column(Modifier.weight(1f).padding(start = 6.dp, end = 4.dp)) {
                Text(
                    t.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (t.done) TextDecoration.LineThrough else null,
                    color = if (t.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (meta.isNotEmpty()) {
                    Text(
                        meta.joinToString("   "),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun pickDateTime(ctx: Context, initial: Long?, onPicked: (Long) -> Unit) {
    val c = Calendar.getInstance().apply { if (initial != null) timeInMillis = initial }
    DatePickerDialog(ctx, { _, y, m, d ->
        TimePickerDialog(ctx, { _, h, min ->
            val r = Calendar.getInstance().apply {
                set(y, m, d, h, min, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onPicked(r.timeInMillis)
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
}

@Composable
private fun SectionLabel(s: String) {
    Text(
        s,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)
    )
}

@Composable
fun EditorScreen(
    init: Task,
    onBack: () -> Unit,
    onSave: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onNeedPerm: () -> Unit
) {
    val ctx = LocalContext.current
    var title by remember { mutableStateOf(init.title) }
    var notes by remember { mutableStateOf(init.notes) }
    var priority by remember { mutableIntStateOf(init.priority) }
    var category by remember { mutableStateOf(init.category) }
    var due by remember { mutableStateOf(init.dueAt) }
    var remind by remember { mutableStateOf(init.remind) }
    var repeat by remember { mutableIntStateOf(init.repeat) }
    val subs = remember { mutableStateListOf<Sub>().apply { addAll(decodeSubs(init.subtasks)) } }
    var newSub by remember { mutableStateOf("") }
    val isNew = init.id == 0L

    fun save() {
        if (title.isBlank()) return
        val all = subs.toList() + (if (newSub.isNotBlank()) listOf(Sub(false, newSub.trim())) else emptyList())
        onSave(
            init.copy(
                title = title.trim(), notes = notes.trim(), priority = priority, category = category,
                dueAt = due, remind = remind && due != null,
                repeat = if (due != null) repeat else 0, subtasks = encodeSubs(all)
            )
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "مهمة جديدة" else "تعديل المهمة") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع") }
                },
                actions = {
                    if (!isNew) IconButton(onClick = { onDelete(init) }) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("عنوان المهمة") }, singleLine = true,
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("ملاحظات") }, minLines = 3,
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
            )

            SectionLabel("الأولوية")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2 to "عالية", 1 to "متوسطة", 0 to "منخفضة").forEach { (p, n) ->
                    FilterChip(
                        selected = priority == p, onClick = { priority = p }, label = { Text(n) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = priorityColor(p).copy(alpha = 0.22f)
                        )
                    )
                }
            }

            SectionLabel("التصنيف")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CATEGORIES.forEach { c ->
                    FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
                }
            }

            SectionLabel("الموعد")
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { pickDateTime(ctx, due) { due = it } },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(due?.let { dueLabel(it) } ?: "تحديد التاريخ والوقت")
                }
                if (due != null) {
                    IconButton(onClick = { due = null; remind = false; repeat = 0 }) {
                        Icon(Icons.Default.Close, contentDescription = "إزالة الموعد")
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("تذكير بإشعار عند الموعد", Modifier.weight(1f))
                Switch(
                    checked = remind && due != null,
                    enabled = due != null,
                    onCheckedChange = { remind = it; if (it) onNeedPerm() }
                )
            }

            SectionLabel("التكرار")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                REPEATS.forEachIndexed { i, n ->
                    FilterChip(selected = repeat == i, onClick = { repeat = i }, enabled = due != null, label = { Text(n) })
                }
            }

            SectionLabel("خطوات فرعية")
            subs.forEachIndexed { i, s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = s.done, onCheckedChange = { subs[i] = s.copy(done = it) })
                    Text(
                        s.text, Modifier.weight(1f),
                        textDecoration = if (s.done) TextDecoration.LineThrough else null
                    )
                    IconButton(onClick = { subs.removeAt(i) }) {
                        Icon(Icons.Default.Close, contentDescription = "حذف الخطوة")
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newSub, onValueChange = { newSub = it },
                    placeholder = { Text("إضافة خطوة") }, singleLine = true,
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    if (newSub.isNotBlank()) { subs.add(Sub(false, newSub.trim())); newSub = "" }
                }) { Icon(Icons.Default.Add, contentDescription = "إضافة") }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { save() },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("حفظ المهمة") }
            Spacer(Modifier.height(24.dp))
        }
    }
}
