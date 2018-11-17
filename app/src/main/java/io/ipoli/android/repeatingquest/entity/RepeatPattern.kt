package io.ipoli.android.repeatingquest.entity

import io.ipoli.android.common.datetime.*
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

    abstract fun periodRangeFor(date: LocalDate): PeriodRange

    abstract fun createPlaceholderDates(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<LocalDate>

    fun createSchedule(
        currentDate: LocalDate,
        lastDate: LocalDate?
    ): Schedule {
        val s = doCreateSchedule(currentDate)

        return s.copy(
            dates = s.dates.filter { date ->
                lastDate?.let {
                    date.isBeforeOrEqual(lastDate)
                } ?: true
            },
            newRepeatPattern = s.newRepeatPattern
        )
    }

    abstract fun doCreateSchedule(
        currentDate: LocalDate
    ): Schedule

    data class Schedule(val dates: List<LocalDate>, val newRepeatPattern: RepeatPattern)

    data class Daily(
        override val startDate: LocalDate = LocalDate.now(),
        override val endDate: LocalDate? = null,
        override val lastScheduledPeriodStart: LocalDate? = null,
        override val skipEveryXPeriods: Int = 0
    ) : RepeatPattern() {

        override fun createPlaceholderDates(
            startDate: LocalDate,
            endDate: LocalDate
        ): List<LocalDate> {
            var periodStart = periodRangeFor(startDate).start
            lastScheduledPeriodStart?.let {
                while (periodStart.isBeforeOrEqual(lastScheduledPeriodStart)) {
                    periodStart = periodStart.plusWeeks(1)
                }
            }
            if (periodStart.isBefore(startDate)) {
                periodStart = startDate
            }
            return periodStart.datesBetween(endDate).filter { shouldDoOn(it) }
        }

        override fun periodRangeFor(date: LocalDate) =
            PeriodRange(
                start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                end = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            )

        override val periodCount get() = DayOfWeek.values().size

        private fun shouldDoOn(date: LocalDate): Boolean {
            val doEveryXDays = skipEveryXPeriods + 1
            if (doEveryXDays == 1) return true
            return startDate.daysUntil(date) % doEveryXDays == 0L
        }

        override fun doCreateSchedule(currentDate: LocalDate): Schedule {
            val nextMonday = currentDate.plusWeeks(1).with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
            )
            return Schedule(
                dates = datesForPeriod(currentDate) + datesForPeriod(nextMonday),
                newRepeatPattern = copy(
                    lastScheduledPeriodStart = nextMonday
                )
            )
        }

        private fun datesForPeriod(
            startDateForPeriod: LocalDate
        ): List<LocalDate> {
            val period = periodRangeFor(startDateForPeriod)
            if (lastScheduledPeriodStart != null && period.start.isBeforeOrEqual(
                    lastScheduledPeriodStart
                )
            ) {
                return emptyList()
            }
            return startDateForPeriod.datesBetween(period.end).filter { shouldDoOn(it) }
        }
    }

    data class Yearly(
        val dayOfMonth: Int,
        val month: Month,
        override val startDate: LocalDate = LocalDate.now(),
        override val endDate: LocalDate? = null,
        override val lastScheduledPeriodStart: LocalDate? = null
    ) : RepeatPattern() {

        override val skipEveryXPeriods: Int = 0
        override val periodCount get() = 1

        override fun createPlaceholderDates(
            startDate: LocalDate,
            endDate: LocalDate
        ): List<LocalDate> {
            var periodStart = periodRangeFor(startDate).start
            lastScheduledPeriodStart?.let {
                while (periodStart.isBeforeOrEqual(lastScheduledPeriodStart)) {
                    periodStart = periodStart.plusYears(1)
                }
            }
            if (periodStart.isBefore(startDate)) {
                periodStart = startDate
            }

            val dates = mutableListOf<LocalDate>()
            while (periodStart.isBeforeOrEqual(endDate)) {
                dates.add(LocalDate.of(periodStart.year, month, dayOfMonth))
                periodStart = periodStart.plusYears(1)
            }

            return dates
        }

        override fun periodRangeFor(date: LocalDate) =
            PeriodRange(
                start = date.with(TemporalAdjusters.firstDayOfYear()),
                end = date.with(TemporalAdjusters.lastDayOfYear())
            )

        override fun doCreateSchedule(currentDate: LocalDate): Schedule {
            val firstOfNextYear = currentDate.plusYears(1)
            return Schedule(
                dates = datesForPeriod(currentDate) + datesForPeriod(firstOfNextYear),
                newRepeatPattern = copy(lastScheduledPeriodStart = firstOfNextYear)
            )
        }

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
        override val lastScheduledPeriodStart: LocalDate? = null,
        override val skipEveryXPeriods: Int = 0
    ) : RepeatPattern() {

        override val periodCount get() = daysOfWeek.size

        override fun createPlaceholderDates(
            startDate: LocalDate,
            endDate: LocalDate
        ): List<LocalDate> {
            var periodStart = periodRangeFor(startDate).start

            lastScheduledPeriodStart?.let {
                while (periodStart.isBeforeOrEqual(lastScheduledPeriodStart)) {
                    periodStart = periodStart.plusWeeks(1)
                }
            }
            if (periodStart.isBefore(startDate)) {
                periodStart = startDate
            }
            val repeatStart = periodRangeFor(this.startDate).start
            return periodStart.datesBetween(endDate).filter {

                val weeksPassed = repeatStart.weeksUntil(periodRangeFor(it).start)
                val doEveryXWeeks = skipEveryXPeriods + 1

                val shouldDoOnWeek = weeksPassed % doEveryXWeeks == 0L

                shouldDoOnWeek && shouldDoOn(it)

            }
        }

        override fun periodRangeFor(date: LocalDate) =
            PeriodRange(
                start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                end = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            )

        override fun doCreateSchedule(currentDate: LocalDate): Schedule {
            val nextMonday = currentDate.plusWeeks(1).with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
            )
            return Schedule(
                dates = datesForPeriod(currentDate) + datesForPeriod(nextMonday),
                newRepeatPattern = copy(lastScheduledPeriodStart = nextMonday)
            )
        }

        private fun shouldDoOn(date: LocalDate) = daysOfWeek.contains(date.dayOfWeek)

        private fun datesForPeriod(startDateForPeriod: LocalDate): List<LocalDate> {
            val period = periodRangeFor(startDateForPeriod)
            val weeksPassed = periodRangeFor(startDate).start.weeksUntil(startDateForPeriod)
            val doEveryXWeeks = skipEveryXPeriods + 1

            val shouldDoOnWeek = weeksPassed % doEveryXWeeks == 0L
            if (!shouldDoOnWeek ||
                (lastScheduledPeriodStart != null &&
                    period.start.isBeforeOrEqual(lastScheduledPeriodStart))
            ) {
                return emptyList()
            }
            return startDateForPeriod.datesBetween(period.end)
                .filter { shouldDoOn(it) }
        }
    }

    data class Monthly(
        val daysOfMonth: Set<Int>,
        override val startDate: LocalDate = LocalDate.now(),
        override val endDate: LocalDate? = null,
        override val lastScheduledPeriodStart: LocalDate? = null,
        override val skipEveryXPeriods: Int = 0
    ) : RepeatPattern() {

        override fun createPlaceholderDates(
            startDate: LocalDate,
            endDate: LocalDate
        ): List<LocalDate> {
            var periodStart = periodRangeFor(startDate).start
            lastScheduledPeriodStart?.let {
                while (periodStart.isBeforeOrEqual(lastScheduledPeriodStart)) {
                    periodStart = periodStart.plusMonths(1)
                }
            }
            if (periodStart.isBefore(startDate)) {
                periodStart = startDate
            }
            val repeatStart = periodRangeFor(this.startDate).start
            return periodStart.datesBetween(endDate).filter {

                val doEveryXMonths = skipEveryXPeriods + 1

                val monthsPassed = repeatStart.monthsUntil(periodRangeFor(it).start)

                val shouldDoOnMonth = monthsPassed % doEveryXMonths == 0L

                shouldDoOnMonth && shouldDoOn(it)
            }
        }

        override val periodCount get() = daysOfMonth.size

        override fun periodRangeFor(date: LocalDate) =
            PeriodRange(
                start = date.with(TemporalAdjusters.firstDayOfMonth()),
                end = date.with(TemporalAdjusters.lastDayOfMonth())
            )

        override fun doCreateSchedule(currentDate: LocalDate): Schedule {
            val nextFirstDayOfMonth = currentDate.plusMonths(1).with(
                TemporalAdjusters.firstDayOfMonth()
            )
            return Schedule(
                dates = datesForPeriod(currentDate) + datesForPeriod(nextFirstDayOfMonth),
                newRepeatPattern = copy(lastScheduledPeriodStart = nextFirstDayOfMonth)
            )
        }

        private fun shouldDoOn(date: LocalDate) = daysOfMonth.contains(date.dayOfMonth)

        private fun datesForPeriod(startDateForPeriod: LocalDate): List<LocalDate> {
            val period = periodRangeFor(startDateForPeriod)
            val doEveryXMonths = skipEveryXPeriods + 1
            val monthsPassed = periodRangeFor(startDate).start.monthsUntil(startDateForPeriod)
            val shouldDoOnMonth = monthsPassed % doEveryXMonths == 0L

            if (!shouldDoOnMonth ||
                (lastScheduledPeriodStart != null &&
                    period.start.isBeforeOrEqual(lastScheduledPeriodStart))
            ) {
                return emptyList()
            }
            return startDateForPeriod.datesBetween(period.end)
                .filter { shouldDoOn(it) }
        }
    }

    sealed class Flexible : RepeatPattern() {

        data class Weekly(
            val timesPerWeek: Int,
            val preferredDays: Set<DayOfWeek> = DayOfWeek.values().toSet(),
            override val startDate: LocalDate = LocalDate.now(),
            override val endDate: LocalDate? = null,
            override val lastScheduledPeriodStart: LocalDate? = null,
            override val skipEveryXPeriods: Int = 0
        ) : Flexible() {

            override fun createPlaceholderDates(
                startDate: LocalDate,
                endDate: LocalDate
            ) = emptyList<LocalDate>()

            override fun periodRangeFor(date: LocalDate) =
                PeriodRange(
                    start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    end = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                )

            override val periodCount get() = timesPerWeek

            override fun doCreateSchedule(currentDate: LocalDate): Schedule {
                val nextMonday = currentDate.plusWeeks(1).with(
                    TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
                )
                return Schedule(
                    dates = datesForPeriod(currentDate) +
                        datesForPeriod(nextMonday),
                    newRepeatPattern = copy(lastScheduledPeriodStart = nextMonday)
                )
            }

            private fun datesForPeriod(
                startDateForPeriod: LocalDate
            ): List<LocalDate> {
                val weeksPassed = periodRangeFor(startDate).start.weeksUntil(startDateForPeriod)
                val doEveryXWeeks = skipEveryXPeriods + 1

                val shouldDoOnWeek = weeksPassed % doEveryXWeeks == 0L
                if (!shouldDoOnWeek ||
                    (lastScheduledPeriodStart != null &&
                        periodRangeFor(startDateForPeriod).start.isBeforeOrEqual(
                            lastScheduledPeriodStart
                        ))
                ) {
                    return emptyList()
                }

                val daysOfWeek = DayOfWeek.values().toList()
                val days = if (preferredDays.isNotEmpty()) {
                    preferredDays.shuffled().take(timesPerWeek)
                } else {
                    daysOfWeek.shuffled().take(timesPerWeek)
                }
                return days
                    .filter { it >= startDateForPeriod.dayOfWeek }
                    .map { startDateForPeriod.with(TemporalAdjusters.nextOrSame(it)) }
            }
        }

        data class Monthly(
            val timesPerMonth: Int,
            val preferredDays: Set<Int>,
            override val startDate: LocalDate = LocalDate.now(),
            override val endDate: LocalDate? = null,
            override val lastScheduledPeriodStart: LocalDate? = null,
            override val skipEveryXPeriods: Int
        ) : Flexible() {

            override fun createPlaceholderDates(
                startDate: LocalDate,
                endDate: LocalDate
            ) = emptyList<LocalDate>()

            override fun periodRangeFor(date: LocalDate) =
                PeriodRange(
                    start = date.with(TemporalAdjusters.firstDayOfMonth()),
                    end = date.with(TemporalAdjusters.lastDayOfMonth())
                )

            override val periodCount get() = timesPerMonth

            override fun doCreateSchedule(
                currentDate: LocalDate
            ): Schedule {
                val nextFirstDayOfMonth = currentDate.plusMonths(1).with(
                    TemporalAdjusters.firstDayOfMonth()
                )
                return Schedule(
                    dates = datesForPeriod(currentDate) + datesForPeriod(nextFirstDayOfMonth),
                    newRepeatPattern = copy(lastScheduledPeriodStart = nextFirstDayOfMonth)
                )
            }

            private fun datesForPeriod(
                startDateForPeriod: LocalDate
            ): List<LocalDate> {
                val period = periodRangeFor(startDateForPeriod)
                val doEveryXMonths = skipEveryXPeriods + 1
                val monthsPassed = periodRangeFor(startDate).start.monthsUntil(startDateForPeriod)
                val shouldDoOnMonth = monthsPassed % doEveryXMonths == 0L

                if (!shouldDoOnMonth ||
                    (lastScheduledPeriodStart != null &&
                        period.start.isBeforeOrEqual(lastScheduledPeriodStart))
                ) {
                    return emptyList()
                }
                val daysOfMonth =
                    (1..period.start.lengthOfMonth()).map { it }.shuffled().take(timesPerMonth)

                val days = if (preferredDays.isNotEmpty()) {
                    preferredDays.shuffled().take(timesPerMonth)
                } else {
                    daysOfMonth.shuffled().take(timesPerMonth)
                }

                return days
                    .filter { it >= startDateForPeriod.dayOfMonth }
                    .map { startDateForPeriod.withDayOfMonth(it) }
            }
        }
    }

    data class Manual(override val startDate: LocalDate = LocalDate.now()) : RepeatPattern() {

        override val endDate: LocalDate? = null

        override val lastScheduledPeriodStart: LocalDate? = null

        override val skipEveryXPeriods: Int = 0

        override val periodCount: Int
            get() = 0

        override fun periodRangeFor(date: LocalDate) = PeriodRange(startDate, LocalDate.now())

        override fun createPlaceholderDates(
            startDate: LocalDate,
            endDate: LocalDate
        ): List<LocalDate> = emptyList()

        override fun doCreateSchedule(currentDate: LocalDate): Schedule {
            return Schedule(emptyList(), this)
        }
    }

}

data class PeriodRange(val start: LocalDate, val end: LocalDate)

data class PeriodProgress(val completedCount: Int, val allCount: Int)

enum class RepeatType {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    MANUAL
}

val RepeatPattern.repeatType: RepeatType
    get() = when (this) {
        is RepeatPattern.Daily -> RepeatType.DAILY
        is RepeatPattern.Weekly -> RepeatType.WEEKLY
        is RepeatPattern.Flexible.Weekly -> RepeatType.WEEKLY
        is RepeatPattern.Monthly -> RepeatType.MONTHLY
        is RepeatPattern.Flexible.Monthly -> RepeatType.MONTHLY
        is RepeatPattern.Yearly -> RepeatType.YEARLY
        is RepeatPattern.Manual -> RepeatType.MANUAL
    }