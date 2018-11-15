package io.ipoli.android.repeatingquest.usecase

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.ipoli.android.TestUtil
import io.ipoli.android.TestUtil.firstDateOfWeek
import io.ipoli.android.TestUtil.lastDateOfWeek
import io.ipoli.android.quest.Quest
import io.ipoli.android.quest.RepeatingQuest
import io.ipoli.android.quest.data.persistence.QuestRepository
import io.ipoli.android.repeatingquest.entity.RepeatPattern
import org.amshove.kluent.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import org.threeten.bp.temporal.TemporalAdjusters
import org.threeten.bp.temporal.WeekFields
import java.util.*

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 02/14/2018.
 */
class SaveQuestsForRepeatingQuestUseCaseSpek : Spek({

    describe("SaveQuestsForRepeatingQuestUseCase") {

        fun doExecuteUseCase(
            quest: RepeatingQuest,
            start: LocalDate,
            questRepo: QuestRepository = TestUtil.questRepoMock()
        ) =
            SaveQuestsForRepeatingQuestUseCase(questRepo, mock()).execute(
                SaveQuestsForRepeatingQuestUseCase.Params(
                    repeatingQuest = quest,
                    start = start
                )
            )

        fun executeUseCaseWithRepeatingQuestResult(
            quest: RepeatingQuest,
            start: LocalDate,
            questRepo: QuestRepository = TestUtil.questRepoMock()
        ) = doExecuteUseCase(quest, start, questRepo).repeatingQuest

        fun executeUseCase(
            quest: RepeatingQuest,
            start: LocalDate,
            questRepo: QuestRepository = TestUtil.questRepoMock()
        ) = doExecuteUseCase(quest, start, questRepo).quests


        fun mockQuestsForRepeatingQuest(quests: List<Quest> = listOf()) =
            mock<QuestRepository> {
                on {
                    findScheduledForRepeatingQuestBetween(
                        any(),
                        any(),
                        any()
                    )
                } doReturn quests
            }

        val questId = "qid"

        fun createRepeatingQuest(start: LocalDate, end: LocalDate? = null) =
            TestUtil.repeatingQuest.copy(
                repeatPattern = RepeatPattern.Daily(startDate = start, endDate = end)
            )

        it("should not schedule anything before quest startDate") {
            val quests = executeUseCase(
                quest = createRepeatingQuest(start = lastDateOfWeek.plusDays(1)),
                start = firstDateOfWeek
            )

            quests.`should be empty`()
        }

        it("should not schedule anything after quest end") {
            val quests = executeUseCase(
                quest = createRepeatingQuest(
                    start = firstDateOfWeek.minusDays(2),
                    end = firstDateOfWeek.minusDays(1)
                ),
                start = firstDateOfWeek
            )

            quests.`should be empty`()
        }

        describe("repeating daily") {

            it("should schedule for every day at startDate of week when no quests are scheduled") {

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest,
                    start = firstDateOfWeek
                )
                quests.size.`should be`(7)
            }

            it("should schedule for every day at startDate of week when 1 quest is scheduled") {

                val repo = mockQuestsForRepeatingQuest(
                    listOf(
                        TestUtil.quest.copy(
                            id = questId,
                            originalScheduledDate = firstDateOfWeek
                        )
                    )
                )

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest,
                    start = firstDateOfWeek,
                    questRepo = repo
                )
                quests.size.`should be`(7)
                quests.first().id.`should be`(questId)
                quests.filter { it.id.isEmpty() }.size.`should be`(6)
            }

            it("should find only stored quests") {


                val repo = mockQuestsForRepeatingQuest(
                    List(7) { i ->
                        TestUtil.quest.copy(
                            id = questId,
                            originalScheduledDate = firstDateOfWeek.plusDays(i.toLong())
                        )
                    }
                )

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest,
                    start = firstDateOfWeek,
                    questRepo = repo
                )
                quests.size.`should be`(7)
                quests.forEach { it.id.`should be`(questId) }
            }

            it("should not schedule for dates with deleted quest") {

                val repo = mockQuestsForRepeatingQuest(
                    listOf(
                        TestUtil.quest.copy(
                            id = questId,
                            originalScheduledDate = firstDateOfWeek,
                            isRemoved = true
                        )
                    )
                )

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest,
                    start = firstDateOfWeek,
                    questRepo = repo
                )
                quests.size.`should be`(6)
                quests.filter { it.scheduledDate!!.isEqual(firstDateOfWeek) }
                    .`should be empty`()
            }

            it("should not schedule for dates after repeating quest end") {

                val repo = mockQuestsForRepeatingQuest(listOf())

                val quests = executeUseCase(
                    quest = createRepeatingQuest(
                        start = firstDateOfWeek,
                        end = firstDateOfWeek.plusDays(3)
                    ),
                    start = firstDateOfWeek,
                    questRepo = repo
                )
                quests.size.`should be`(4)
            }

            it("should not schedule for dates before repeating quest startDate") {

                val repo = mockQuestsForRepeatingQuest(listOf())

                val quests = executeUseCase(
                    quest = createRepeatingQuest(
                        start = firstDateOfWeek.plusDays(1),
                        end = lastDateOfWeek
                    ),
                    start = firstDateOfWeek,
                    questRepo = repo
                )
                quests.size.`should be`(6)
            }

        }

        describe("repeating weekly") {

            describe("fixed") {

                it("should schedule for every week day in pattern") {

                    val repo = mockQuestsForRepeatingQuest(listOf())

                    val quests = executeUseCase(
                        quest = TestUtil.repeatingQuest.copy(
                            repeatPattern = RepeatPattern.Weekly(
                                setOf(
                                    DayOfWeek.MONDAY,
                                    DayOfWeek.WEDNESDAY,
                                    DayOfWeek.FRIDAY
                                ),
                                firstDateOfWeek
                            )
                        ),
                        start = firstDateOfWeek,
                        questRepo = repo
                    )
                    quests.size.`should be`(5)
                    quests.filter { it.id.isEmpty() }.size.`should be`(5)
                }

                it("should not schedule for moved stored quest") {

                    val repo = mockQuestsForRepeatingQuest(
                        listOf(
                            TestUtil.quest.copy(
                                id = questId,
                                originalScheduledDate = firstDateOfWeek,
                                scheduledDate = firstDateOfWeek.plusDays(1)
                            )
                        )
                    )

                    val quests = executeUseCase(
                        quest = TestUtil.repeatingQuest.copy(
                            repeatPattern = RepeatPattern.Weekly(
                                setOf(
                                    DayOfWeek.MONDAY,
                                    DayOfWeek.WEDNESDAY,
                                    DayOfWeek.FRIDAY
                                ),
                                firstDateOfWeek
                            )
                        ),
                        start = firstDateOfWeek,
                        questRepo = repo
                    )
                    quests.size.`should be`(3)
                    quests.filter { it.id.isEmpty() }.size.`should be`(2)
                }
            }

            describe("flexible") {

                fun createQuest(
                    timesPerWeek: Int,
                    preferredDays: Set<DayOfWeek> = setOf()
                ): RepeatingQuest {
                    return TestUtil.repeatingQuest.copy(
                        repeatPattern = RepeatPattern.Flexible.Weekly(
                            timesPerWeek = timesPerWeek,
                            preferredDays = preferredDays,
                            startDate = firstDateOfWeek
                        )
                    )
                }

                it("should not allow 1 time per week with 1 preferred day") {


                    val exec = {

                        executeUseCase(
                            createQuest(
                                timesPerWeek = 1,
                                preferredDays = setOf(DayOfWeek.MONDAY)
                            ),
                            firstDateOfWeek
                        )
                    }
                    exec shouldThrow IllegalArgumentException::class
                }

                it("should not allow to repeat less than once per week") {
                    val exec = {
                        executeUseCase(
                            createQuest(
                                timesPerWeek = 0,
                                preferredDays = setOf(DayOfWeek.MONDAY)
                            ),
                            firstDateOfWeek
                        )
                    }
                    exec shouldThrow IllegalArgumentException::class
                }

                it("should not allow to repeat more than 7 times per week") {
                    val exec = {
                        executeUseCase(
                            createQuest(
                                timesPerWeek = 8,
                                preferredDays = setOf(DayOfWeek.MONDAY)
                            ),
                            firstDateOfWeek
                        )
                    }
                    exec shouldThrow IllegalArgumentException::class
                }

                it("should schedule 1 quest without preferred days") {
                    val quests = executeUseCase(
                        createQuest(
                            timesPerWeek = 1
                        ),
                        firstDateOfWeek
                    )

                    quests.size.`should be`(1)
                }

                it("should schedule 1 quest with 2 preferred days") {
                    val preferredDays = setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
                    val quests = executeUseCase(
                        createQuest(
                            timesPerWeek = 1,
                            preferredDays = preferredDays
                        ),
                        firstDateOfWeek
                    )

                    quests.size.`should be`(1)
                    val scheduledDate = quests.first().scheduledDate
                    scheduledDate!!.dayOfWeek.`should be in`(preferredDays)
                }

                it("should schedule 2 quest with 1 preferred day") {
                    val preferredDays = setOf(DayOfWeek.FRIDAY)
                    val quests = executeUseCase(
                        createQuest(
                            timesPerWeek = 2,
                            preferredDays = preferredDays
                        ),
                        firstDateOfWeek
                    )

                    quests.size.`should be`(2)
                    val scheduledDates = quests.map { it.scheduledDate }
                    scheduledDates.filter { it!!.dayOfWeek == DayOfWeek.FRIDAY }.size.`should be`(1)
                }

                it("should schedule 3 quest with 4 preferred day") {
                    val preferredDays = setOf(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY
                    )
                    val quests = executeUseCase(
                        createQuest(
                            timesPerWeek = 3,
                            preferredDays = preferredDays
                        ),
                        firstDateOfWeek.with(DayOfWeek.WEDNESDAY)
                    )

                    quests.size.`should be in range`(1, 2)
                    val possibleWeekDays = setOf(
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY
                    )
                    if (quests.size == 1) {
                        quests.first().scheduledDate!!.dayOfWeek.`should be in`(
                            possibleWeekDays
                        )
                    } else {
                        quests.map { it.scheduledDate!!.dayOfWeek }.`should contain all`(
                            possibleWeekDays
                        )
                    }
                }

                it("should schedule 5 quests for 2 days") {
                    val quests = executeUseCase(
                        createQuest(
                            timesPerWeek = 6
                        ),
                        firstDateOfWeek.with(DayOfWeek.SATURDAY)
                    )

                    quests.size.`should be in range`(1, 2)
                    val possibleWeekDays = setOf(
                        DayOfWeek.SATURDAY,
                        DayOfWeek.SUNDAY
                    )
                    if (quests.size == 1) {
                        quests.first().scheduledDate!!.dayOfWeek.`should be in`(
                            possibleWeekDays
                        )
                    } else {
                        quests.map { it.scheduledDate!!.dayOfWeek }.`should contain all`(
                            possibleWeekDays
                        )
                    }
                }

                it("should schedule quests in different weeks") {
                    val possibleWeekDays = setOf(
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.FRIDAY
                    )
                    val quests = executeUseCase(
                        createQuest(
                            timesPerWeek = 1,
                            preferredDays = possibleWeekDays
                        ),
                        firstDateOfWeek.with(DayOfWeek.THURSDAY)
                    )

                    quests.size.`should be in range`(1, 2)
                    if (quests.size == 1) {
                        quests.first().scheduledDate!!.dayOfWeek.`should be in`(
                            possibleWeekDays
                        )
                    } else {
                        quests.forEach {
                            it.scheduledDate!!.dayOfWeek.`should be in`(possibleWeekDays)
                        }
                        val first = quests.first().scheduledDate
                        val second = quests[1].scheduledDate
                        val woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()
                        first!!.get(woy).`should not be`(second!!.get(woy))
                    }
                }

                it("should add 1 scheduled period") {
                    val rq = executeUseCaseWithRepeatingQuestResult(
                        createQuest(
                            timesPerWeek = 1
                        ),
                        firstDateOfWeek
                    )

                    val scheduledPeriods =
                        (rq.repeatPattern as RepeatPattern.Flexible.Weekly).scheduledPeriods
                    scheduledPeriods.size.`should be`(1)
                    scheduledPeriods.keys.first().`should be`(firstDateOfWeek)
                    scheduledPeriods[firstDateOfWeek]!!.size.`should be`(1)

                }

                it("should add 2 scheduled periods") {
                    val rq = executeUseCaseWithRepeatingQuestResult(
                        createQuest(
                            timesPerWeek = 3
                        ),
                        firstDateOfWeek.plusDays(1)
                    )

                    val scheduledPeriods =
                        (rq.repeatPattern as RepeatPattern.Flexible.Weekly).scheduledPeriods
                    scheduledPeriods.size.`should be`(2)
                    scheduledPeriods.keys.`should contain all`(
                        listOf(
                            firstDateOfWeek,
                            lastDateOfWeek.plusDays(1)
                        )
                    )
                }

                it("should add 1 scheduled periods and not override the other") {
                    val scheduledPeriodDates = listOf(
                        firstDateOfWeek,
                        firstDateOfWeek.plusDays(1),
                        firstDateOfWeek.plusDays(2)
                    )

                    val rq = executeUseCaseWithRepeatingQuestResult(
                        createQuest(
                            timesPerWeek = 3
                        ),
                        firstDateOfWeek.plusDays(1)
                    )

                    val scheduledPeriods =
                        (rq.repeatPattern as RepeatPattern.Flexible.Weekly).scheduledPeriods
                    scheduledPeriods.size.`should be`(2)
                    scheduledPeriods[firstDateOfWeek]!!.`should contain all`(scheduledPeriodDates)
                }
            }
        }

        describe("repeating monthly") {

            describe("fixed") {

                it("should schedule for every month day in pattern when days present in month") {

                    val repo = mockQuestsForRepeatingQuest(listOf())

                    val start = LocalDate.of(2000, 2, 1)
                    val end = LocalDate.of(2000, 3, 15)


                    val quests = executeUseCase(
                        quest = TestUtil.repeatingQuest.copy(
                            repeatPattern = RepeatPattern.Monthly(
                                setOf(1, 10, 29, 31), start
                            )
                        ),
                        start = start,
                        questRepo = repo
                    )
                    quests.size.`should be`(5)
                    quests.filter { it.id.isEmpty() }.size.`should be`(5)
                }

                it("should not schedule for stored quests") {

                    val start = LocalDate.of(2000, 2, 1)

                    val repo = mockQuestsForRepeatingQuest(
                        listOf(
                            TestUtil.quest.copy(
                                id = questId,
                                originalScheduledDate = start
                            )
                        )
                    )

                    val quests = executeUseCase(
                        quest = TestUtil.repeatingQuest.copy(
                            repeatPattern = RepeatPattern.Monthly(
                                setOf(1, 10, 29, 31), start
                            )
                        ),
                        start = start,
                        questRepo = repo
                    )
                    quests.size.`should be`(5)
                    quests.filter { it.id.isEmpty() }.size.`should be`(4)
                }
            }

            describe("flexible") {
                val firstJanuary = LocalDate.now().withMonth(1).withDayOfMonth(1)
                val lastJanuary = firstJanuary.with(TemporalAdjusters.lastDayOfMonth())
                val firstFebruary = LocalDate.now().withMonth(2).withDayOfMonth(1)
                val lastFebruary = firstFebruary.with(TemporalAdjusters.lastDayOfMonth())

                fun createQuest(
                    timesPerMonth: Int,
                    preferredDays: Set<Int> = setOf()
                ): RepeatingQuest {
                    return TestUtil.repeatingQuest.copy(
                        repeatPattern = RepeatPattern.Flexible.Monthly(
                            timesPerMonth = timesPerMonth,
                            preferredDays = preferredDays,
                            startDate = firstJanuary
                        )
                    )
                }

                it("should not allow 1 time per month with 1 preferred day") {

                    val exec = {

                        executeUseCase(
                            createQuest(
                                timesPerMonth = 1,
                                preferredDays = setOf(1)
                            ),
                            firstDateOfWeek
                        )
                    }
                    exec shouldThrow IllegalArgumentException::class
                }

                it("should not allow to repeat less than once per month") {
                    val exec = {
                        executeUseCase(
                            createQuest(
                                timesPerMonth = 0,
                                preferredDays = setOf(1)
                            ),
                            firstDateOfWeek
                        )
                    }
                    exec shouldThrow IllegalArgumentException::class
                }

                it("should not allow to repeat more than 31 times per month") {
                    val exec = {
                        executeUseCase(
                            createQuest(
                                timesPerMonth = 32,
                                preferredDays = setOf(1)
                            ),
                            firstDateOfWeek
                        )
                    }
                    exec shouldThrow IllegalArgumentException::class
                }

                it("should schedule 1 quest without preferred days") {
                    val quests = executeUseCase(
                        createQuest(
                            timesPerMonth = 1
                        ),
                        firstJanuary
                    )

                    quests.size.`should be`(1)
                }

                it("should schedule 1 quest with 2 preferred days") {
                    val preferredDays = setOf(10, 20)
                    val quests = executeUseCase(
                        createQuest(
                            timesPerMonth = 1,
                            preferredDays = preferredDays
                        ),
                        firstJanuary
                    )
                    quests.size.`should be`(1)
                    val scheduledDate = quests.first().scheduledDate
                    scheduledDate!!.dayOfMonth.`should be in`(preferredDays)
                }

                it("should schedule 2 quest with 1 preferred day") {
                    val preferredDays = setOf(10)
                    val quests = executeUseCase(
                        createQuest(
                            timesPerMonth = 2,
                            preferredDays = preferredDays
                        ),
                        firstJanuary
                    )

                    quests.size.`should be`(2)
                    val scheduledDates = quests.map { it.scheduledDate }
                    scheduledDates.filter { it!!.dayOfMonth == 10 }.size.`should be`(1)
                }

                it("should schedule 3 quest with 4 preferred day") {
                    val preferredDays = setOf(10, 12, 20, 25)
                    val quests = executeUseCase(
                        createQuest(
                            timesPerMonth = 3,
                            preferredDays = preferredDays
                        ),
                        firstJanuary.plusDays(15)
                    )

                    quests.size.`should be in range`(1, 2)
                    val possibleMonthDays = setOf(20, 25)
                    if (quests.size == 1) {
                        quests.first().scheduledDate!!.dayOfMonth.`should be in`(
                            possibleMonthDays
                        )
                    } else {
                        quests.map { it.scheduledDate!!.dayOfMonth }.`should contain all`(
                            possibleMonthDays
                        )
                    }
                }

                it("should schedule 30 quests for 2 days") {
                    val quests = executeUseCase(
                        createQuest(
                            timesPerMonth = 30
                        ),
                        firstJanuary.plusDays(29)
                    )

                    quests.size.`should be in range`(1, 2)
                    val possibleMonthDays = setOf(30, 31)
                    if (quests.size == 1) {
                        quests.first().scheduledDate!!.dayOfMonth.`should be in`(
                            possibleMonthDays
                        )
                    } else {
                        quests.map { it.scheduledDate!!.dayOfMonth }.`should contain all`(
                            possibleMonthDays
                        )
                    }
                }

                it("should schedule 30 quests in February") {
                    val quests = executeUseCase(
                        createQuest(
                            timesPerMonth = 30
                        ),
                        firstFebruary
                    )

                    quests.size.`should be`(firstFebruary.lengthOfMonth())
                }

                it("should schedule quests in different months") {
                    val possibleMonthDays = setOf(10, 20)
                    val quests = executeUseCase(
                        createQuest(
                            timesPerMonth = 1,
                            preferredDays = possibleMonthDays
                        ),
                        firstJanuary.plusDays(15)
                    )

                    quests.size.`should be in range`(1, 2)
                    if (quests.size == 1) {
                        val scheduledDate = quests.first().scheduledDate
                        scheduledDate!!.dayOfMonth.`should be in`(possibleMonthDays)
                        scheduledDate.month.`should be`(Month.FEBRUARY)
                    } else {
                        quests.forEach {
                            it.scheduledDate!!.dayOfMonth.`should be in`(possibleMonthDays)
                        }
                        val first = quests.first().scheduledDate
                        val second = quests[1].scheduledDate
                        first!!.monthValue.`should not be equal to`(second!!.monthValue)
                    }
                }

                it("should add 1 scheduled period") {
                    val rq = executeUseCaseWithRepeatingQuestResult(
                        createQuest(
                            timesPerMonth = 1
                        ),
                        firstJanuary
                    )

                    val scheduledPeriods =
                        (rq.repeatPattern as RepeatPattern.Flexible.Monthly).scheduledPeriods
                    scheduledPeriods.size.`should be`(1)
                    scheduledPeriods.keys.first().`should be`(firstJanuary)
                    scheduledPeriods[firstJanuary]!!.size.`should be`(1)

                }

                it("should add 2 scheduled periods") {
                    val rq = executeUseCaseWithRepeatingQuestResult(
                        createQuest(
                            timesPerMonth = 3
                        ),
                        firstJanuary.plusDays(7)
                    )

                    val scheduledPeriods =
                        (rq.repeatPattern as RepeatPattern.Flexible.Monthly).scheduledPeriods
                    scheduledPeriods.size.`should be`(2)
                    scheduledPeriods.keys.`should contain all`(
                        listOf(
                            firstJanuary,
                            firstFebruary
                        )
                    )
                }

                it("should add 1 scheduled periods and not override the other") {
                    val scheduledPeriodDates = listOf(
                        firstFebruary,
                        firstFebruary.plusDays(1),
                        firstFebruary.plusDays(2)
                    )

                    val rq = executeUseCaseWithRepeatingQuestResult(
                        createQuest(
                            timesPerMonth = 3
                        ),
                        firstJanuary.plusDays(7)
                    )

                    val scheduledPeriods =
                        (rq.repeatPattern as RepeatPattern.Flexible.Monthly).scheduledPeriods
                    scheduledPeriods.size.`should be`(2)
                    scheduledPeriods[firstFebruary]!!.`should contain all`(scheduledPeriodDates)
                }
            }
        }

        describe("repeating yearly") {

            it("should not schedule for date outside range") {
                val repo = mockQuestsForRepeatingQuest(
                    listOf(
                        TestUtil.quest.copy(
                            id = questId,
                            originalScheduledDate = firstDateOfWeek
                        )
                    )
                )

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest.copy(
                        repeatPattern = RepeatPattern.Yearly(
                            firstDateOfWeek.dayOfMonth,
                            firstDateOfWeek.month,
                            firstDateOfWeek
                        )
                    ),
                    start = firstDateOfWeek.plusDays(1),
                    questRepo = repo
                )
                quests.`should be empty`()
            }

            it("should create when no quest is scheduled") {
                val repo = mockQuestsForRepeatingQuest(listOf())

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest.copy(
                        repeatPattern = RepeatPattern.Yearly(
                            firstDateOfWeek.dayOfMonth,
                            firstDateOfWeek.month,
                            firstDateOfWeek
                        )
                    ),
                    start = firstDateOfWeek,
                    questRepo = repo
                )
                quests.size.`should be`(1)
                quests.first().id.`should be empty`()
            }

            it("should return scheduled quest") {
                val repo = mockQuestsForRepeatingQuest(
                    listOf(
                        TestUtil.quest.copy(
                            id = questId,
                            originalScheduledDate = firstDateOfWeek
                        )
                    )
                )

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest.copy(
                        repeatPattern = RepeatPattern.Yearly(
                            firstDateOfWeek.dayOfMonth,
                            firstDateOfWeek.month,
                            firstDateOfWeek
                        )
                    ),
                    start = firstDateOfWeek,
                    questRepo = repo
                )
                quests.size.`should be`(1)
                quests.first().id.`should be`(questId)
            }

            it("should not schedule for dates with deleted quest") {

                val repo = mockQuestsForRepeatingQuest(
                    listOf(
                        TestUtil.quest.copy(
                            id = questId,
                            originalScheduledDate = firstDateOfWeek,
                            isRemoved = true
                        )
                    )
                )

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest.copy(
                        repeatPattern = RepeatPattern.Yearly(
                            firstDateOfWeek.dayOfMonth,
                            firstDateOfWeek.month,
                            firstDateOfWeek
                        )
                    ),
                    start = firstDateOfWeek,
                    questRepo = repo
                )
                quests.`should be empty`()
            }

            it("should schedule quests for 3 years") {
                val start = firstDateOfWeek
                val repo = mockQuestsForRepeatingQuest(listOf())

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest.copy(
                        repeatPattern = RepeatPattern.Yearly(
                            firstDateOfWeek.plusDays(1).dayOfMonth,
                            firstDateOfWeek.plusDays(1).month,
                            firstDateOfWeek
                        )
                    ),
                    start = start,
                    questRepo = repo
                )
                quests.size.`should be`(3)
                quests.filter { it.id.isEmpty() }.size.`should be`(3)
            }

            it("should schedule 1 quest and use 2 stored quests for 3 years") {
                val start = firstDateOfWeek
                val repo = mockQuestsForRepeatingQuest(
                    listOf(
                        TestUtil.quest.copy(
                            id = "1",
                            originalScheduledDate = firstDateOfWeek.plusDays(1)
                        ),
                        TestUtil.quest.copy(
                            id = "2",
                            originalScheduledDate = firstDateOfWeek.plusDays(1).plusYears(2)
                        )
                    )
                )

                val quests = executeUseCase(
                    quest = TestUtil.repeatingQuest.copy(
                        repeatPattern = RepeatPattern.Yearly(
                            firstDateOfWeek.plusDays(1).dayOfMonth,
                            firstDateOfWeek.plusDays(1).month,
                            firstDateOfWeek
                        )
                    ),
                    start = start,
                    questRepo = repo
                )
                quests.size.`should be`(3)
                quests.filter { it.id.isEmpty() }.size.`should be`(1)
            }
        }
    }
})