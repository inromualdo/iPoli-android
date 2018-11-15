package io.ipoli.android.repeatingquest.entity

import io.ipoli.android.common.datetime.datesBetween
import io.ipoli.android.common.datetime.daysUntil
import io.ipoli.android.common.datetime.isBeforeOrEqual
import io.ipoli.android.common.datetime.isBetween
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import org.threeten.bp.temporal.TemporalAdjusters

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 02/14/2018.
 */

sealed class RepeatPattern {
    abstract val startDate: LocalDate
    abstract val endDate: LocalDate?
    abstract val lastScheduledPeriodStart: LocalDate?
    abstract val skipEveryXPeriods: Int
    abstract val periodCount: Int
    abstract val scheduleAheadPeriods: Int

    abstract fun periodRangeFor(date: LocalDate): PeriodRange
    abstract fun datesToScheduleOn(currentDate: LocalDate): List<LocalDate>

    data class Daily(
        override val startDate: LocalDate = LocalDate.now(),
        override val endDate: LocalDate? = null,
        override val lastScheduledPeriodStart: LocalDate?,
        override val skipEveryXPeriods: Int
    ) : RepeatPattern() {

        override val scheduleAheadPeriods: Int = 1

        override fun periodRangeFor(date: LocalDate) =
            PeriodRange(
                start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                end = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            )

        override val periodCount get() = DayOfWeek.values().size

        private fun shouldBeDoneOn(date: LocalDate) =
            startDate.daysUntil(date) % skipEveryXPeriods == 0L

        override fun datesToScheduleOn(currentDate: LocalDate) =
            datesForPeriod(currentDate) + datesForPeriod(currentDate.plusWeeks(1))

        private fun datesForPeriod(
            dateInPeriod: LocalDate
        ): List<LocalDate> {
            val period = periodRangeFor(dateInPeriod)
            if (lastScheduledPeriodStart != null && period.start.isBeforeOrEqual(
                    lastScheduledPeriodStart
                )
            ) {
                return emptyList()
            }
            return dateInPeriod.datesBetween(period.end).filter { shouldBeDoneOn(it) }
        }
    }

    data class Yearly(
        val dayOfMonth: Int,
        val month: Month,
        override val startDate: LocalDate = LocalDate.now(),
        override val endDate: LocalDate? = null,
        override val lastScheduledPeriodStart: LocalDate?
    ) : RepeatPattern() {

        override val skipEveryXPeriods: Int = 0
        override val scheduleAheadPeriods: Int = 1
        override val periodCount get() = 1

        override fun periodRangeFor(date: LocalDate) =
            PeriodRange(
                start = date.with(TemporalAdjusters.firstDayOfYear()),
                end = date.with(TemporalAdjusters.lastDayOfYear())
            )

        override fun datesToScheduleOn(currentDate: LocalDate) =
            datesForPeriod(currentDate) + datesForPeriod(currentDate.plusYears(1))

        private fun datesForPeriod(
            dateInPeriod: LocalDate
        ): List<LocalDate> {
            val period = periodRangeFor(dateInPeriod)
            if (lastScheduledPeriodStart != null && period.start.isBeforeOrEqual(
                    lastScheduledPeriodStart
                )
            ) {
                return emptyList()
            }
            return listOf(LocalDate.of(dateInPeriod.year, month, dayOfMonth))
        }
    }

    data class Weekly(
        val daysOfWeek: Set<DayOfWeek>,
        override val startDate: LocalDate = LocalDate.now(),
        override val endDate: LocalDate? = null,
        override val lastScheduledPeriodStart: LocalDate?,
        override val skipEveryXPeriods: Int
    ) : RepeatPattern() {
        override fun periodRangeFor(date: LocalDate) =
            PeriodRange(
                start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                end = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            )

        override val periodCount get() = daysOfWeek.size
    }

    data class Monthly(
        val daysOfMonth: Set<Int>,
        override val startDate: LocalDate = LocalDate.now(),
        override val endDate: LocalDate? = null,
        override val lastScheduledPeriodStart: LocalDate?,
        override val skipEveryXPeriods: Int
    ) : RepeatPattern() {

        override fun periodRangeFor(date: LocalDate) =
            PeriodRange(
                start = date.with(TemporalAdjusters.firstDayOfMonth()),
                end = date.with(TemporalAdjusters.lastDayOfMonth())
            )

        override val periodCount get() = daysOfMonth.size
    }

    sealed class Flexible : RepeatPattern() {

        data class Weekly(
            val timesPerWeek: Int,
            val preferredDays: Set<DayOfWeek> = DayOfWeek.values().toSet(),
            override val startDate: LocalDate = LocalDate.now(),
            override val endDate: LocalDate? = null,
            override val lastScheduledPeriodStart: LocalDate? = null,
            override val skipEveryXPeriods: Int
        ) : Flexible() {

            override fun periodRangeFor(date: LocalDate) =
                PeriodRange(
                    start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    end = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                )

            override val periodCount get() = timesPerWeek
        }

        data class Monthly(
            val timesPerMonth: Int,
            val preferredDays: Set<Int>,
            override val startDate: LocalDate = LocalDate.now(),
            override val endDate: LocalDate? = null,
            override val lastScheduledPeriodStart: LocalDate? = null,
            override val skipEveryXPeriods: Int
        ) : Flexible() {
            override fun periodRangeFor(date: LocalDate) =
                PeriodRange(
                    start = date.with(TemporalAdjusters.firstDayOfMonth()),
                    end = date.with(TemporalAdjusters.lastDayOfMonth())
                )

            override val periodCount get() = timesPerMonth
        }
    }


    companion object {
        fun findWeeklyPeriods(
            start: LocalDate,
            end: LocalDate
        ): List<Period> {

            val periods = mutableListOf<Period>()
            val firstDayOfWeek = DayOfWeek.MONDAY

            var periodStart = start.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
            val dayAfterEnd = end.plusDays(1)
            while (periodStart.isBefore(dayAfterEnd)) {
                val periodEnd = periodStart.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                periods.add(Period(periodStart, periodEnd))
                periodStart = periodEnd.plusDays(1)
            }

            return periods
        }

        fun findMonthlyPeriods(
            start: LocalDate,
            end: LocalDate
        ): List<Period> {
            val periods = mutableListOf<Period>()

            var periodStart = start.with(TemporalAdjusters.firstDayOfMonth())
            val dayAfterEnd = end.plusDays(1)
            while (periodStart.isBefore(dayAfterEnd)) {
                val periodEnd = periodStart.with(TemporalAdjusters.lastDayOfMonth())
                periods.add(Period(periodStart, periodEnd))
                periodStart = periodEnd.plusDays(1)
            }

            return periods
        }

        fun monthlyDatesToScheduleInPeriod(
            repeatPattern: RepeatPattern.Monthly,
            start: LocalDate,
            end: LocalDate
        ): List<LocalDate> {

            var date = start
            val dates = mutableListOf<LocalDate>()
            while (date.isBefore(end.plusDays(1))) {
                if (date.dayOfMonth in repeatPattern.daysOfMonth) {
                    dates.add(date)
                }
                date = date.plusDays(1)
            }
            return dates

        }

        fun weeklyDatesToScheduleInPeriod(
            repeatPattern: RepeatPattern.Weekly,
            start: LocalDate,
            end: LocalDate
        ): List<LocalDate> {

            var date = start
            val dates = mutableListOf<LocalDate>()
            while (date.isBefore(end.plusDays(1))) {
                if (date.dayOfWeek in repeatPattern.daysOfWeek) {
                    dates.add(date)
                }
                date = date.plusDays(1)
            }
            return dates

        }

        fun yearlyDatesToScheduleInPeriod(
            repeatPattern: RepeatPattern.Yearly,
            start: LocalDate,
            end: LocalDate
        ): List<LocalDate> {
            if (start.year == end.year) {
                val date = LocalDate.of(
                    start.year,
                    repeatPattern.month,
                    repeatPattern.dayOfMonth
                )
                return listOf(date).filter { it.isBetween(start, end) }
            }

            var startPeriodDate = start
            val dates = mutableListOf<LocalDate>()
            while (startPeriodDate <= end) {
                val lastDayOfYear = LocalDate.of(startPeriodDate.year, 12, 31)
                val date = LocalDate.of(
                    startPeriodDate.year,
                    repeatPattern.month,
                    repeatPattern.dayOfMonth
                )
                val endPeriodDate = if (end.isBefore(lastDayOfYear)) end else lastDayOfYear
                if (date.isBetween(startPeriodDate, endPeriodDate)) {
                    dates.add(date)
                }
                startPeriodDate = LocalDate.of(startPeriodDate.year + 1, 1, 1)
            }
            return dates

        }
    }
}

data class PeriodRange(val start: LocalDate, val end: LocalDate)

data class PeriodProgress(val completedCount: Int, val allCount: Int)

data class Period(val start: LocalDate, val end: LocalDate)

enum class RepeatType {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

val RepeatPattern.repeatType: RepeatType
    get() = when (this) {
        is RepeatPattern.Daily -> RepeatType.DAILY
        is RepeatPattern.Weekly -> RepeatType.WEEKLY
        is RepeatPattern.Flexible.Weekly -> RepeatType.WEEKLY
        is RepeatPattern.Monthly -> RepeatType.MONTHLY
        is RepeatPattern.Flexible.Monthly -> RepeatType.MONTHLY
        is RepeatPattern.Yearly -> RepeatType.YEARLY
    }