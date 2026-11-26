package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.GlassTint
import com.example.reminders.NoteReminderScheduler
import com.example.ui.glass.LiquidGlassCard
import com.example.ui.theme.tr
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteReminderDialog(
    currentReminderAt: Long,
    isDark: Boolean,
    isArabic: Boolean,
    onConfirmReminder: (Long) -> Unit,
    onClearReminder: () -> Unit,
    onPermissionDenied: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val initialMillis = remember(currentReminderAt) {
        if (currentReminderAt > System.currentTimeMillis()) {
            currentReminderAt
        } else {
            Calendar.getInstance().apply {
                add(Calendar.HOUR_OF_DAY, 1)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }

    var selectedMillis by remember(currentReminderAt) { mutableLongStateOf(initialMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingSaveMillis by remember { mutableStateOf<Long?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            onPermissionDenied()
        }
        val target = pendingSaveMillis ?: selectedMillis
        pendingSaveMillis = null
        onConfirmReminder(target)
    }

    val amberAccent = Color(0xFFF59E0B)
    val roseWarn = Color(0xFFF43F5E)
    val titleColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val surfaceTile = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF0F172A).copy(alpha = 0.05f)
    val locale = if (isArabic) Locale("ar") else Locale.ENGLISH
    val nowMillis = System.currentTimeMillis()
    val isSelectedPast = selectedMillis <= nowMillis

    val dateFormatted = remember(selectedMillis, isArabic) {
        SimpleDateFormat("EEE, dd MMM yyyy", locale).format(Date(selectedMillis))
    }
    val timeFormatted = remember(selectedMillis) {
        SimpleDateFormat("HH:mm", Locale.US).format(Date(selectedMillis))
    }

    fun applyQuickOffset(minutesFromNow: Int) {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MINUTE, minutesFromNow)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        selectedMillis = cal.timeInMillis
    }

    fun applyTargetHour(daysAheadIfPast: Boolean, hour24: Int, minute: Int) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!daysAheadIfPast) {
                add(Calendar.DAY_OF_YEAR, 1)
            } else if (!after(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        selectedMillis = target.timeInMillis
    }

    fun confirmWithPermissionCheck() {
        val target = selectedMillis
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NoteReminderScheduler.hasNotificationPermission(context)
        ) {
            pendingSaveMillis = target
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onConfirmReminder(target)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isDark) 0.58f else 0.36f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassCard(
                isDark = isDark,
                tint = GlassTint.AMBER,
                shape = RoundedCornerShape(26.dp),
                borderWidth = 1.6.dp,
                testTag = "note_reminder_dialog",
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth(0.92f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(amberAccent.copy(alpha = if (isDark) 0.22f else 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = tr("تذكير", "Rappel", "Reminder", "Asmekti"),
                                tint = amberAccent,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("تذكير المسودة", "Rappel du brouillon", "Draft Reminder", "Asmekti n tira"),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = titleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = NoteReminderScheduler.formatReminderFullText(selectedMillis, isArabic),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelectedPast) roseWarn else amberAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Quick-pick chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReminderQuickChip(
                            label = tr("+15 دقيقة", "+15 min", "+15m", "+15 min"),
                            tint = amberAccent,
                            textColor = titleColor,
                            bgColor = surfaceTile,
                            testTag = "reminder_quick_15m",
                            onClick = { applyQuickOffset(15) }
                        )
                        ReminderQuickChip(
                            label = tr("+1 ساعة", "+1 h", "+1h", "+1 s"),
                            tint = amberAccent,
                            textColor = titleColor,
                            bgColor = surfaceTile,
                            testTag = "reminder_quick_1h",
                            onClick = { applyQuickOffset(60) }
                        )
                        ReminderQuickChip(
                            label = tr("الليلة 20:00", "Ce soir 20h", "Tonight 20:00", "Iḍ-a 20:00"),
                            tint = amberAccent,
                            textColor = titleColor,
                            bgColor = surfaceTile,
                            testTag = "reminder_quick_tonight",
                            onClick = { applyTargetHour(daysAheadIfPast = true, hour24 = 20, minute = 0) }
                        )
                        ReminderQuickChip(
                            label = tr("غداً 09:00", "Demain 09h", "Tomorrow 09:00", "Azekka 09:00"),
                            tint = amberAccent,
                            textColor = titleColor,
                            bgColor = surfaceTile,
                            testTag = "reminder_quick_tomorrow",
                            onClick = { applyTargetHour(daysAheadIfPast = false, hour24 = 9, minute = 0) }
                        )
                    }

                    // Date & Time selection buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1.35f)
                                .minimumInteractiveComponentSize()
                                .heightIn(min = 48.dp)
                                .testTag("reminder_date_picker_btn")
                                .clip(RoundedCornerShape(14.dp))
                                .background(surfaceTile)
                                .border(
                                    width = 1.dp,
                                    color = amberAccent.copy(alpha = 0.28f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) { showDatePicker = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = tr("اختر التاريخ", "Choisir la date", "Choose Date", "Fren azemz"),
                                tint = amberAccent,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = dateFormatted,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = titleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(0.85f)
                                .minimumInteractiveComponentSize()
                                .heightIn(min = 48.dp)
                                .testTag("reminder_time_picker_btn")
                                .clip(RoundedCornerShape(14.dp))
                                .background(surfaceTile)
                                .border(
                                    width = 1.dp,
                                    color = amberAccent.copy(alpha = 0.28f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) { showTimePicker = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = tr("اختر الوقت", "Choisir l'heure", "Choose Time", "Fren akud"),
                                tint = amberAccent,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = timeFormatted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = titleColor,
                                maxLines = 1
                            )
                        }
                    }

                    // Past time warning
                    if (isSelectedPast) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(roseWarn.copy(alpha = if (isDark) 0.18f else 0.10f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = roseWarn,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = tr(
                                    "الوقت المحدد في الماضي، اختر وقتاً مستقبلياً",
                                    "L'heure choisie est passée, choisissez un horaire futur",
                                    "Selected time is in the past — choose a future time",
                                    "Akud-a iɛedda, fren akud i d-iteddun"
                                ),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = roseWarn
                            )
                        }
                    }

                    HorizontalDivider(
                        color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
                    )

                    // Bottom Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentReminderAt > 0L) {
                            TextButton(
                                onClick = onClearReminder,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("reminder_clear_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = tr("إلغاء التذكير", "Supprimer le rappel", "Clear Reminder", "Kkes asmekti"),
                                    tint = roseWarn,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tr("إزالة", "Effacer", "Clear", "Kkes"),
                                    color = roseWarn,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("reminder_cancel_button")
                            ) {
                                Text(
                                    text = tr("إلغاء", "Annuler", "Cancel", "Sefsex"),
                                    color = subColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }

                            Button(
                                onClick = { confirmWithPermissionCheck() },
                                enabled = !isSelectedPast,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = amberAccent,
                                    contentColor = Color(0xFF0F172A)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .heightIn(min = 48.dp)
                                    .testTag("reminder_confirm_button")
                            ) {
                                Text(
                                    text = tr("تعيين", "Définir", "Set Reminder", "Sers"),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val currentCal = remember(selectedMillis) {
            Calendar.getInstance().apply { timeInMillis = selectedMillis }
        }
        val utcDateMillis = remember(selectedMillis) {
            Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH), currentCal.get(Calendar.DAY_OF_MONTH))
            }.timeInMillis
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = utcDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { pickedUtc ->
                            val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = pickedUtc
                            }
                            val merged = Calendar.getInstance().apply {
                                timeInMillis = selectedMillis
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                            }
                            selectedMillis = merged.timeInMillis
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(tr("تأكيد", "OK", "OK", "Ih"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(tr("إلغاء", "Annuler", "Cancel", "Sefsex"))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val currentCal = remember(selectedMillis) {
            Calendar.getInstance().apply { timeInMillis = selectedMillis }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = currentCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = currentCal.get(Calendar.MINUTE),
            is24Hour = true
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            LiquidGlassCard(
                isDark = isDark,
                tint = GlassTint.FROST,
                shape = RoundedCornerShape(24.dp),
                borderWidth = 1.4.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = tr("تحديد الوقت", "Choisir l'heure", "Select Time", "Fren akud"),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = titleColor
                        )
                    )
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(tr("إلغاء", "Annuler", "Cancel", "Sefsex"), color = subColor)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val merged = Calendar.getInstance().apply {
                                    timeInMillis = selectedMillis
                                    set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                    set(Calendar.MINUTE, timePickerState.minute)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                selectedMillis = merged.timeInMillis
                                showTimePicker = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = amberAccent,
                                contentColor = Color(0xFF0F172A)
                            )
                        ) {
                            Text(tr("تأكيد", "OK", "OK", "Ih"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderQuickChip(
    label: String,
    tint: Color,
    textColor: Color,
    bgColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minHeight = 40.dp)
            .testTag(testTag)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, tint.copy(alpha = 0.35f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = tint),
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1
        )
    }
}
