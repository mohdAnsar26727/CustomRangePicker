package com.example.mylibrary.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mylibrary.util.extensions.DateFormat
import com.example.mylibrary.util.extensions.excludeTime
import com.example.mylibrary.util.extensions.resetDayOfMonth
import com.example.mylibrary.util.extensions.toFormattedDateLabel
import com.example.mylibrary.widget.CustomDateRangePickerStateImpl.Companion.toCalendarDateModel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

data class CalendarDate(
    val dayOfMonth: Int,
    val month: Int,
    val year: Int,
    val timestamp: Long,
    val isSelectable: Boolean
)

@Immutable
data class CustomDateRangePickerColors(
    val selectionStartEndColor: Color,
    val todayDateIndicatorColor: Color,
    val rangeDateColor: Color,
    val selectionStartEndTextColor: Color,
    val dateTextColor: Color
)

@Immutable
data class CustomDateRangePickerTextStyle(
    val dateTextStyle: TextStyle,
    val weekTextStyle: TextStyle
)

interface SelectableDates {
    fun isSelectableDate(utcTimeMillis: Long) = true
}

@Stable
interface CustomDateRangePickerState {

    val selectedStartDate: CalendarDate?

    val selectedEndDate: CalendarDate?

    val displayedMonth: Calendar

    val initialPage: Int

    val today: CalendarDate

    val yearRange: IntRange

    val selectableDates: SelectableDates

    fun setSelection(startDate: CalendarDate?, endDate: CalendarDate?)

    fun isInRange(timestamp: Long): Boolean

    fun getCalendarByPageIndex(page: Int): Calendar

    fun setDisplayedMonth(page: Int)

    fun getDisplayedMonthDates(calendar: Calendar): List<CalendarDate>
}

object CustomDateRangePickerDefault {

    val AllDates = object : SelectableDates {}

    @Composable
    fun ColumnScope.CalendarHeader(
        selectedMonth: CalendarDate,
        onMonthChange: (NavigationAction) -> Unit
    ) {
        HorizontalDivider()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = selectedMonth.timestamp.toFormattedDateLabel(DateFormat.MMMM_YYYY),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            val backwardIcon = Icons.AutoMirrored.Default.KeyboardArrowLeft
            val forwardIcon = Icons.AutoMirrored.Default.KeyboardArrowRight
            val buttonShape = RoundedCornerShape(30)

            listOf(
                backwardIcon,
                forwardIcon,
            ).forEach { icon ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(buttonShape)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainer,
                            buttonShape
                        )
                        .clickable {
                            onMonthChange(
                                if (icon == forwardIcon) {
                                    NavigationAction.Next
                                } else {
                                    NavigationAction.Previous
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null
                    )
                }
            }
        }

        HorizontalDivider()
    }

    @Composable
    fun ColumnScope.CalenderFooter(
        state: CustomDateRangePickerState
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                Pair(
                    "From",
                    state.selectedStartDate
                ),
                Pair(
                    "To",
                    state.selectedEndDate
                ),
            ).forEach { (label, date) ->
                DateSelectionIndicator(
                    label = label,
                    selectedFormattedDate = date?.timestamp?.toFormattedDateLabel(DateFormat.DD_MMM_YYYY),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    val YearRange: IntRange = IntRange(1900, 2100)
}


internal class CustomDateRangePickerStateImpl(
    initialSelectedStartDate: Long? = null,
    initialSelectedEndDate: Long? = null,
    initialDisplayedMonthMillis: Long? = null,
    override val yearRange: IntRange,
    override val selectableDates: SelectableDates
) : CustomDateRangePickerState {

    val calenderInstance get() = Calendar.getInstance(TimeZone.getTimeZone("UTC")).excludeTime()

    val utcCalender: Calendar = calenderInstance

    override val initialPage: Int

    override val today: CalendarDate

    init {

        if (initialSelectedStartDate != null && initialSelectedEndDate != null) {
            setSelection(
                calenderInstance.apply {
                    time = Date(initialSelectedStartDate)
                }.toCalendarDateModel(selectableDates),
                calenderInstance.apply {
                    time = Date(initialSelectedEndDate)
                }.toCalendarDateModel(selectableDates),
            )
        }

        val initialCalendar =
            if (initialDisplayedMonthMillis != null) calenderInstance.apply {
                time = Date(initialDisplayedMonthMillis)
            } else utcCalender

        val startYear = yearRange.first
        val currentYear = initialCalendar.get(Calendar.YEAR)
        val currentMonthValue = initialCalendar.get(Calendar.MONTH)

        // Calculate total months from the start year to the current year and month
        val monthsFromStart =
            (currentYear - startYear) * 12 + (currentMonthValue - 1) // -1 to make it zero-based
        initialPage = monthsFromStart
        today = utcCalender.toCalendarDateModel(selectableDates)
    }

    private val _selectedStartDate = mutableStateOf<CalendarDate?>(null)

    private val _selectedEndDate = mutableStateOf<CalendarDate?>(null)

    private var _displayedMonth = mutableStateOf<Calendar>(utcCalender)

    override val selectedStartDate: CalendarDate? get() = _selectedStartDate.value

    override val selectedEndDate: CalendarDate? get() = _selectedEndDate.value

    override val displayedMonth: Calendar get() = _displayedMonth.value

    override fun setSelection(startDate: CalendarDate?, endDate: CalendarDate?) {
        _selectedStartDate.value = startDate
        _selectedEndDate.value = endDate
    }

    override fun getCalendarByPageIndex(page: Int): Calendar {
        val offsetFromInitialPage = page - initialPage
        val totalMonths = utcCalender.get(Calendar.MONTH) + offsetFromInitialPage
        val baseYear = utcCalender.get(Calendar.YEAR) + totalMonths / 12
        val baseMonth = (totalMonths % 12).let { if (it < 0) it + 12 else it }

        return calenderInstance.apply {
            set(Calendar.YEAR, baseYear)
            set(Calendar.MONTH, baseMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    override fun setDisplayedMonth(page: Int) {
        _displayedMonth.value = getCalendarByPageIndex(page)
    }

    override fun getDisplayedMonthDates(calendar: Calendar): List<CalendarDate> =
        (1..calendar.getActualMaximum(Calendar.DAY_OF_MONTH)).map {
            val date = calendar.apply {
                set(
                    Calendar.DAY_OF_MONTH,
                    it
                )
            }
            CalendarDate(
                it,
                date.get(Calendar.MONTH),
                date.get(Calendar.YEAR),
                date.timeInMillis,
                selectableDates.isSelectableDate(date.timeInMillis)
            )
        }

    override fun isInRange(
        timestamp: Long
    ): Boolean {
        return selectedStartDate?.timestamp != selectedEndDate?.timestamp
                && selectedStartDate != null
                && selectedEndDate != null
                && timestamp in selectedStartDate!!.timestamp..selectedEndDate!!.timestamp
    }

    companion object {

        fun Calendar.toCalendarDateModel(selectableDates: SelectableDates) = CalendarDate(
            get(Calendar.DAY_OF_MONTH),
            get(Calendar.MONTH),
            get(Calendar.YEAR),
            timeInMillis,
            selectableDates.isSelectableDate(timeInMillis)
        )

        fun Saver(
            selectableDates: SelectableDates
        ): Saver<CustomDateRangePickerStateImpl, Any> =
            listSaver(
                save = {
                    listOf(
                        it.selectedStartDate?.timestamp,
                        it.selectedEndDate?.timestamp,
                        it.displayedMonth.timeInMillis,
                        it.yearRange.first,
                        it.yearRange.last
                    )
                },
                restore = { value ->
                    CustomDateRangePickerStateImpl(
                        initialSelectedStartDate = value[0] as Long?,
                        initialSelectedEndDate = value[1] as Long?,
                        initialDisplayedMonthMillis = value[2] as Long?,
                        yearRange = IntRange(value[3] as Int, value[4] as Int),
                        selectableDates = selectableDates
                    )
                }
            )
    }
}

@Composable
fun rememberCustomDateRangePickerState(
    initialSelectedStartDate: Long? = null,
    initialSelectedEndDate: Long? = null,
    initialDisplayedMonthMillis: Long? = null,
    yearRange: IntRange = CustomDateRangePickerDefault.YearRange,
    selectableDates: SelectableDates = CustomDateRangePickerDefault.AllDates
): CustomDateRangePickerState {
    return rememberSaveable(saver = CustomDateRangePickerStateImpl.Saver(selectableDates)) {
        CustomDateRangePickerStateImpl(
            initialSelectedStartDate,
            initialSelectedEndDate,
            initialDisplayedMonthMillis,
            yearRange,
            selectableDates
        )
    }
}

enum class NavigationAction {
    Previous,
    Next
}

@Composable
fun CustomDateRangePicker(
    state: CustomDateRangePickerState,
    modifier: Modifier = Modifier,
    colors: CustomDateRangePickerColors,
    textStyles: CustomDateRangePickerTextStyle,
    header: @Composable ColumnScope.(
        selectedMonth: CalendarDate,
        onMonthChange: (NavigationAction) -> Unit,
    ) -> Unit = { selectedMonth, onMonthChange ->
        with(CustomDateRangePickerDefault) {
            CalendarHeader(selectedMonth, onMonthChange)
        }
    },
    footer: @Composable ColumnScope.() -> Unit = {
        with(CustomDateRangePickerDefault) {
            CalenderFooter(state)
        }
    }
) {

    var cellSize by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val pagerState = rememberPagerState(initialPage = state.initialPage) { Int.MAX_VALUE }
    val scope = rememberCoroutineScope()
    val onMonthChange: (NavigationAction) -> Unit = { action ->
        when (action) {
            NavigationAction.Previous -> {
                scope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                }
            }

            NavigationAction.Next -> {
                scope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        // Header with Month Navigation
        header(state.displayedMonth.toCalendarDateModel(state.selectableDates), onMonthChange)

        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        style = textStyles.weekTextStyle.copy(
                            platformStyle = PlatformTextStyle(
                                includeFontPadding = false
                            )
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.wrapContentSize()
                    )
                }

            }
        }
        // Calculate the initial page

        LaunchedEffect(pagerState.currentPage) {
            state.setDisplayedMonth(pagerState.currentPage)
        }

        HorizontalPager(pagerState) { page ->

            val calendar = state.getCalendarByPageIndex(page)

            val dayOfWeek = calendar.resetDayOfMonth().get(Calendar.DAY_OF_WEEK)
            val placeHolders = if (dayOfWeek == Calendar.SUNDAY) 0 else dayOfWeek - 1

            Layout(modifier = Modifier
                .height(cellSize * 6),
                content = {
                    // Placeholder items to align the first day of the month
                    repeat(placeHolders) {
                        Spacer(Modifier.weight(1f))
                    }

                    // Calendar days
                    state.getDisplayedMonthDates(calendar).forEach { date ->

                        val isStart = date.timestamp == state.selectedStartDate?.timestamp
                        val isEnd = date.timestamp == state.selectedEndDate?.timestamp
                        val isToday = date.timestamp == state.today.timestamp
                        val isInRange = state.isInRange(date.timestamp)

                        // Modifier with drawBehind to handle background, rounded corners, and borders
                        val finalModifier = buildDateItemModifier(
                            isToday,
                            isStart,
                            isEnd,
                            isInRange,
                            colors
                        ) { width, height ->
                            if (cellSize == 0.dp) {
                                cellSize = with(density) { height.toDp() }
                            }
                        }

                        Box(
                            modifier = finalModifier,
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                modifier = Modifier.run {
                                    if (date.isSelectable) {
                                        clickable {
                                            handleDateClick(
                                                state,
                                                date
                                            )
                                        }
                                    } else {
                                        alpha(0.5f)
                                    }
                                },
                                text = date.dayOfMonth.toString(),
                                fontSize = 16.sp,
                                style = textStyles.dateTextStyle,
                                color = if (isStart || isEnd) colors.selectionStartEndTextColor else colors.dateTextColor
                            )
                        }
                    }
                })
            { measurables, constraints ->
                val columns = 7
                val cellWidth = constraints.maxWidth / columns
                val cellConstraints = constraints.copy(
                    minWidth = cellWidth,
                    maxWidth = cellWidth,
                    minHeight = cellWidth,
                    maxHeight = cellWidth
                )

                // Measure each item
                val placeables = measurables.map { measurable ->
                    measurable.measure(cellConstraints)
                }

                val rows = (placeables.size + columns - 1) / columns
                val layoutHeight = rows * cellWidth
                val yOffset = (constraints.maxHeight - layoutHeight) / 2// Center vertically

                layout(
                    constraints.maxWidth,
                    layoutHeight
                ) {
                    placeables.forEachIndexed { index, placeable ->
                        val column = index % columns
                        val row = index / columns
                        val x = column * cellWidth
                        val y = row * cellWidth - yOffset
                        placeable.placeRelative(
                            x,
                            y
                        )
                    }
                }
            }
        }

        footer()

    }
}


@OptIn(ExperimentalMaterial3Api::class)
private fun handleDateClick(
    state: CustomDateRangePickerState,
    calendarDate: CalendarDate
) {
    when {
        state.selectedStartDate == null && state.selectedEndDate == null -> state.setSelection(
            calendarDate,
            null
        )

        state.selectedStartDate != null && calendarDate.timestamp >= state.selectedStartDate!!.timestamp -> state.setSelection(
            state.selectedStartDate,
            calendarDate
        )

        else -> state.setSelection(
            calendarDate,
            null
        )
    }
}

@Composable
fun DateSelectionIndicator(
    label: String,
    selectedFormattedDate: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.alpha(if (selectedFormattedDate == null) 0.5f else 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainer,
                    RoundedCornerShape(30)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null
            )
        }

        val text = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    fontStyle = MaterialTheme.typography.titleSmall.fontStyle,
                    color = Color.Gray.copy(0.5f),
                    fontSize = 12.sp
                )
            ) {
                append(label)
            }
            append("\n")
            withStyle(
                style = SpanStyle(
                    fontStyle = MaterialTheme.typography.titleSmall.fontStyle,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            ) {
                append(selectedFormattedDate ?: "Select Date")
            }
        }

        Text(text)
    }
}

@Composable
fun buildDateItemModifier(
    isToday: Boolean,
    isStart: Boolean,
    isEnd: Boolean,
    isInRange: Boolean,
    colors: CustomDateRangePickerColors,
    afterCellSizeCalculation: (width: Int, height: Int) -> Unit
): Modifier {
    val density = LocalDensity.current
    return Modifier
        .aspectRatio(1f)
        .onGloballyPositioned { coordinates ->
            afterCellSizeCalculation(
                coordinates.size.width,
                coordinates.size.height
            )
        }
        .drawBehind {
            val rectSize = size.copy(height = size.height - 1.dp.toPx())
            // Step 1: Draw the rectangular background if in range
            if (isInRange) {
                // If it's the start, apply rounded corners on the left
                if (isStart) {
                    drawOutline(
                        RoundedCornerShape(
                            topStart = 100f,
                            bottomStart = 100f
                        ).createOutline(
                            size = rectSize,
                            density = density,
                            layoutDirection = layoutDirection
                        ),
                        color = colors.rangeDateColor
                    )
                }
                // If it's the end, apply rounded corners on the right
                else if (isEnd) {
                    drawOutline(
                        RoundedCornerShape(
                            topEnd = 100f,
                            bottomEnd = 100f
                        ).createOutline(
                            size = rectSize,
                            density = density,
                            layoutDirection = layoutDirection
                        ),
                        color = colors.rangeDateColor
                    )
                }
                // If neither is start nor end, just draw a full rectangle
                else {
                    drawRect(
                        color = colors.rangeDateColor,
                        size = rectSize
                    )
                }
            }

            // Step 2: Draw the circle in the center for start or end
            if (isStart || isEnd) {
                val circleRadius = size.minDimension / 2f - 4.dp.toPx()

                drawCircle(
                    color = colors.todayDateIndicatorColor,
                    radius = circleRadius,
                    center = Offset(
                        size.width / 2f,
                        size.height / 2f
                    )
                )
            }

            // Step 3: Draw borders for today's date if necessary
            if (isToday) {
                drawCircle(
                    color = colors.selectionStartEndColor,
                    radius = size.minDimension / 2f - 4.dp.toPx(),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
}

val weekDays get() = setOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")