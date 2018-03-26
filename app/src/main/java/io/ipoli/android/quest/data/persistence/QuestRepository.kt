package io.ipoli.android.quest.data.persistence

import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import io.ipoli.android.common.datetime.*
import io.ipoli.android.common.persistence.BaseCollectionFirestoreRepository
import io.ipoli.android.common.persistence.CollectionRepository
import io.ipoli.android.common.persistence.FirestoreModel
import io.ipoli.android.pet.Food
import io.ipoli.android.quest.*
import org.threeten.bp.*
import kotlin.coroutines.experimental.CoroutineContext

interface QuestRepository : CollectionRepository<Quest> {
    fun listenForScheduledBetween(
        startDate: LocalDate,
        endDate: LocalDate
    ): ReceiveChannel<List<Quest>>

    fun listenForScheduledAt(date: LocalDate): ReceiveChannel<List<Quest>>
    fun findScheduledAt(date: LocalDate): List<Quest>
    fun findScheduledForRepeatingQuestBetween(
        repeatingQuestId: String,
        start: LocalDate,
        end: LocalDate
    ): List<Quest>

    fun findNextReminderTime(afterTime: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())): LocalDateTime?

    fun findQuestsToRemind(remindTime: LocalDateTime): List<Quest>
    fun findCompletedForDate(date: LocalDate): List<Quest>
    fun findStartedQuests(): List<Quest>
    fun findLastScheduledDate(currentDate: LocalDate, maxQuests: Int): LocalDate?
    fun findFirstScheduledDate(currentDate: LocalDate, maxQuests: Int): LocalDate?
    fun findNextScheduledNotCompletedForRepeatingQuest(
        repeatingQuestId: String,
        currentDate: LocalDate
    ): Quest?

    fun findAllForRepeatingQuest(
        repeatingQuestId: String,
        includeRemoved: Boolean = true
    ): List<Quest>

    fun findOriginalScheduledForRepeatingQuestAtDate(
        repeatingQuestId: String,
        currentDate: LocalDate
    ): Quest?

    fun findCompletedCountForRepeatingQuestInPeriod(
        repeatingQuestId: String,
        start: LocalDate,
        end: LocalDate?
    ): Int

    fun findCompletedForRepeatingQuestInPeriod(
        repeatingQuestId: String,
        start: LocalDate,
        end: LocalDate? = null
    ): List<Quest>

    fun purgeAllNotCompletedForRepeating(
        repeatingQuestId: String,
        startDate: LocalDate = LocalDate.now()
    )

    fun findNotCompletedNotForChallengeNotRepeating(
        challengeId: String,
        start: LocalDate = LocalDate.now()
    ): List<Quest>

    fun findAllForChallengeNotRepeating(challengeId: String): List<Quest>

    fun findAllForChallenge(challengeId: String): List<Quest>
    fun findAllForRepeatingQuestAfterDate(
        repeatingQuestId: String,
        includeRemoved: Boolean,
        currentDate: LocalDate = LocalDate.now()
    ): List<Quest>

    fun purge(questId: String)

    fun purge(questIds: List<String>)
}

data class DbQuest(override val map: MutableMap<String, Any?> = mutableMapOf()) :
    FirestoreModel {
    override var id: String by map
    var name: String by map
    var color: String by map
    var icon: String? by map
    var category: String by map
    var duration: Int by map
    var reminder: MutableMap<String, Any?>? by map
    var startMinute: Long? by map
    var experience: Long? by map
    var coins: Long? by map
    var bounty: MutableMap<String, Any?>? by map
    var scheduledDate: Long by map
    var originalScheduledDate: Long by map
    var completedAtDate: Long? by map
    var completedAtMinute: Long? by map
    var timeRanges: List<MutableMap<String, Any?>> by map
    var timeRangeCount: Int by map
    var repeatingQuestId: String? by map
    var challengeId: String? by map
    override var createdAt: Long by map
    override var updatedAt: Long by map
    override var removedAt: Long? by map
}

data class DbReminder(val map: MutableMap<String, Any?> = mutableMapOf()) {
    var message: String by map
    var minute: Int by map
    var date: Long? by map
}

data class DbBounty(val map: MutableMap<String, Any?> = mutableMapOf()) {
    var type: String by map
    var name: String? by map

    enum class Type {
        NONE, FOOD
    }
}

data class DbTimeRange(val map: MutableMap<String, Any?> = mutableMapOf()) {
    var type: String by map
    var duration: Int by map
    var start: Long? by map
    var end: Long? by map
}

class FirestoreQuestRepository(
    database: FirebaseFirestore,
    coroutineContext: CoroutineContext,
    sharedPreferences: SharedPreferences
) : BaseCollectionFirestoreRepository<Quest, DbQuest>(
    database,
    coroutineContext,
    sharedPreferences
), QuestRepository {

    override fun findAllForRepeatingQuestAfterDate(
        repeatingQuestId: String,
        includeRemoved: Boolean,
        currentDate: LocalDate
    ): List<Quest> {
        val query = collectionReference
            .whereEqualTo("repeatingQuestId", repeatingQuestId)
            .whereGreaterThanOrEqualTo("scheduledDate", currentDate.startOfDayUTC())
        return if (includeRemoved)
            toEntityObjects(query.documents)
        else
            query.entities
    }

    /**
     * Includes removed Quests
     */
    override fun findAllForChallenge(challengeId: String) =
        toEntityObjects(
            collectionReference
                .whereEqualTo("challengeId", challengeId)
                .documents
        )

    override fun findNotCompletedNotForChallengeNotRepeating(
        challengeId: String,
        start: LocalDate
    ): List<Quest> {
        val quests = collectionReference
            .whereEqualTo("repeatingQuestId", null)
            .whereEqualTo("completedAtDate", null)
            .whereGreaterThanOrEqualTo("scheduledDate", start.startOfDayUTC()).entities

        return quests.filter { it.challengeId != challengeId }
    }

    override fun findAllForRepeatingQuest(
        repeatingQuestId: String,
        includeRemoved: Boolean
    ): List<Quest> {

        val query = collectionReference.whereEqualTo("repeatingQuestId", repeatingQuestId)
        return if (includeRemoved)
            toEntityObjects(query.documents)
        else
            query.entities
    }


    override fun purgeAllNotCompletedForRepeating(
        repeatingQuestId: String,
        startDate: LocalDate
    ) =
        collectionReference
            .whereEqualTo("repeatingQuestId", repeatingQuestId)
            .whereEqualTo("completedAtDate", null)
            .whereGreaterThanOrEqualTo("scheduledDate", startDate.startOfDayUTC())
            .documents
            .map { it.id }
            .let { purge(it) }

    override fun findAllForChallengeNotRepeating(challengeId: String) =
        collectionReference
            .whereEqualTo("challengeId", challengeId)
            .whereEqualTo("repeatingQuestId", null)
            .entities


    override fun findCompletedForRepeatingQuestInPeriod(
        repeatingQuestId: String,
        start: LocalDate,
        end: LocalDate?
    ) = createCompletedForRepeatingInPeriodQuery(repeatingQuestId, start, end).entities

    override fun findCompletedCountForRepeatingQuestInPeriod(
        repeatingQuestId: String,
        start: LocalDate,
        end: LocalDate?
    ) = createCompletedForRepeatingInPeriodQuery(repeatingQuestId, start, end).documents.size

    private fun createCompletedForRepeatingInPeriodQuery(
        repeatingQuestId: String,
        start: LocalDate,
        end: LocalDate?
    ): Query {
        var ref = collectionReference
            .whereEqualTo("repeatingQuestId", repeatingQuestId)
            .whereGreaterThanOrEqualTo("completedAtDate", start.startOfDayUTC())
        if (end != null) {
            ref = ref.whereLessThanOrEqualTo("completedAtDate", end.startOfDayUTC())
        }
        return ref
    }

    override fun findNextScheduledNotCompletedForRepeatingQuest(
        repeatingQuestId: String,
        currentDate: LocalDate
    ) =
        collectionReference
            .whereEqualTo("repeatingQuestId", repeatingQuestId)
            .whereGreaterThanOrEqualTo("scheduledDate", currentDate.startOfDayUTC())
            .whereEqualTo("completedAtDate", null)
            .orderBy("scheduledDate", Query.Direction.ASCENDING)
            .limit(1)
            .entities.firstOrNull()

    override fun findOriginalScheduledForRepeatingQuestAtDate(
        repeatingQuestId: String,
        currentDate: LocalDate
    ): Quest? {
        val doc = collectionReference
            .whereEqualTo("repeatingQuestId", repeatingQuestId)
            .whereEqualTo("originalScheduledDate", currentDate.startOfDayUTC())
            .limit(1)
            .execute().documents
        if (doc.isEmpty()) {
            return null
        }
        return toEntityObject(doc.first().data)
    }

    override fun listenForScheduledBetween(
        startDate: LocalDate,
        endDate: LocalDate
    ) =
        collectionReference
            .whereGreaterThanOrEqualTo("scheduledDate", startDate.startOfDayUTC())
            .whereLessThanOrEqualTo("scheduledDate", endDate.startOfDayUTC())
            .orderBy("scheduledDate")
            .orderBy("startMinute")
            .listenForChanges()

    override fun findScheduledAt(date: LocalDate) =
        collectionReference
            .whereGreaterThan("scheduledDate", date.startOfDayUTC() - 1)
            .whereLessThanOrEqualTo("scheduledDate", date.startOfDayUTC())
            .orderBy("scheduledDate")
            .orderBy("startMinute")
            .entities

    override fun findScheduledForRepeatingQuestBetween(
        repeatingQuestId: String,
        start: LocalDate,
        end: LocalDate
    ) =
        collectionReference
            .whereEqualTo("repeatingQuestId", repeatingQuestId)
            .whereGreaterThan("scheduledDate", start.startOfDayUTC() - 1)
            .whereLessThanOrEqualTo("scheduledDate", end.startOfDayUTC()).entities

    override fun listenForScheduledAt(date: LocalDate) =
        collectionReference
            .whereEqualTo("scheduledDate", date.startOfDayUTC())
            .listenForChanges()

    override fun findNextReminderTime(afterTime: ZonedDateTime): LocalDateTime? {

        val currentDateMillis = afterTime.toLocalDate().startOfDayUTC()

        val millisOfDay = afterTime.toLocalTime().toSecondOfDay().seconds.millisValue

        val query =
            remindersReference
                .orderBy("date")
                .orderBy("millisOfDay")
                .startAt(
                    currentDateMillis,
                    millisOfDay + 1
                )
                .limit(1)

        val documents = query.documents
        if (documents.isEmpty()) {
            return null
        }

        val reminder = documents[0]

        val remindDate = (reminder.get("date") as Long).startOfDayUTC
        val remindMillis = reminder.get("millisOfDay") as Long
        return LocalDateTime.of(
            remindDate,
            LocalTime.ofSecondOfDay(remindMillis.milliseconds.asSeconds.longValue)
        )
    }

    override fun findQuestsToRemind(remindTime: LocalDateTime): List<Quest> {
        val query = remindersReference
            .whereEqualTo("date", remindTime.toLocalDate().startOfDayUTC())
            .whereEqualTo(
                "millisOfDay", remindTime.toLocalTime().toSecondOfDay().seconds.millisValue
            )
        val documents = query.documents
        if (documents.isEmpty()) {
            return listOf()
        }
        val questIds = documents.map {
            it["questId"]
        }

        var questRef: Query = collectionReference
        questIds.forEach {
            questRef = questRef.whereEqualTo("id", it)
        }
        return questRef.entities
    }

    override fun findStartedQuests(): List<Quest> {
        val query = collectionReference
            .whereEqualTo("completedAtDate", null)
            .whereGreaterThan("timeRangeCount", 0)
        return query.entities
    }

    override fun findCompletedForDate(date: LocalDate): List<Quest> {
        val query = collectionReference
            // Due to Firestore bug (kinda) we can't query using the same value as data
            // see https://stackoverflow.com/a/47379643/6336582
            .whereGreaterThan("completedAtDate", date.startOfDayUTC() - 1)
            .whereLessThanOrEqualTo("completedAtDate", date.startOfDayUTC())
        return query.entities
    }

    override fun findLastScheduledDate(currentDate: LocalDate, maxQuests: Int): LocalDate? {
        val endDateQuery = collectionReference
            .whereGreaterThan("scheduledDate", currentDate.startOfDayUTC())
            .limit(maxQuests.toLong())
            .orderBy("scheduledDate", Query.Direction.ASCENDING)
        val endDateQuests = endDateQuery.entities

        if (endDateQuests.isEmpty()) {
            return null
        }

        return endDateQuests.last().scheduledDate
    }

    override fun findFirstScheduledDate(currentDate: LocalDate, maxQuests: Int): LocalDate? {
        val startDateQuery = collectionReference
            .whereLessThan("scheduledDate", currentDate.startOfDayUTC())
            .limit(maxQuests.toLong())
            .orderBy("scheduledDate", Query.Direction.DESCENDING)
        val startDateQuests = startDateQuery.entities

        if (startDateQuests.isEmpty()) {
            return null
        }

        return startDateQuests.last().scheduledDate
    }

    override val collectionReference
        get() = database.collection("players").document(playerId).collection("quests")

    private val remindersReference
        get() = database.collection("players").document(playerId).collection("reminders")

    override fun save(entity: Quest): Quest {
        val quest = super.save(entity)
        quest.reminder?.let {
            saveReminders(quest.id, listOf(it))
        }
        return quest
    }

    override fun save(entities: List<Quest>): List<Quest> {
        val quests = super.save(entities)

        val batch = database.batch()

        val questToReminder = quests.map { Pair(it.id, it.reminder) }.filter { it.second != null }

        val questIds = questToReminder.map { it.first }

        purgeReminders(questIds, batch)

        questToReminder.forEach {
            val ref = remindersReference.document()
            batch.set(ref, createReminderData(it.first, it.second!!))
        }

        batch.commit()
        return quests
    }

    private fun purgeReminders(
        questIds: List<String>,
        batch: WriteBatch
    ) {
        var allRemindersQuery: Query = remindersReference

        questIds.forEach {
            allRemindersQuery = allRemindersQuery.whereEqualTo("questId", it)
        }

        allRemindersQuery.documents.forEach {
            val ref = remindersReference.document(it.id)
            batch.delete(ref)
        }
    }

    private fun saveReminders(questId: String, reminders: List<Reminder>) {
        purgeReminders(questId)
        addReminders(reminders, questId)
    }

    private fun addReminders(
        reminders: List<Reminder>,
        questId: String
    ) {
        reminders.forEach {
            remindersReference.add(createReminderData(questId, it))
        }
    }

    private fun createReminderData(
        questId: String,
        reminder: Reminder
    ): Map<String, Any> {
        val r = mapOf(
            "questId" to questId,
            "date" to reminder.remindDate!!.startOfDayUTC(),
            "millisOfDay" to reminder.remindTime.toMillisOfDay()
        )
        return r
    }

    private fun purgeReminders(questId: String) {
        val batch = database.batch()

        val query = remindersReference.whereEqualTo("questId", questId)
        query.documents.forEach {
            val ref = remindersReference.document(it.id)
            batch.delete(ref)
        }
        batch.commit()
    }

    override fun remove(id: String) {
        super.remove(id)
        purgeReminders(id)
    }

    override fun undoRemove(id: String) {
        super.undoRemove(id)
        val quest = findById(id)!!
        quest.reminder?.let {
            addReminders(listOf(it), id)
        }
    }

    override fun purge(questId: String) {
        purgeReminders(questId)
        collectionReference.document(questId).delete()
    }

    override fun purge(questIds: List<String>) {
        val batch = database.batch()
        questIds.forEach {
            val ref = collectionReference.document(it)
            batch.delete(ref)
        }

        purgeReminders(questIds, batch)

        batch.commit()
    }

    override fun toEntityObject(dataMap: MutableMap<String, Any?>): Quest {
        val cq = DbQuest(dataMap.withDefault {
            null
        })

        val plannedDate = cq.scheduledDate.startOfDayUTC
        val plannedTime = cq.startMinute?.let { Time.of(it.toInt()) }

        return Quest(
            id = cq.id,
            name = cq.name,
            color = Color.valueOf(cq.color),
            icon = cq.icon?.let {
                Icon.valueOf(it)
            },
            category = Category(cq.category, Color.GREEN),
            scheduledDate = plannedDate,
            originalScheduledDate = cq.originalScheduledDate.startOfDayUTC,
            startTime = plannedTime,
            duration = cq.duration,
            experience = cq.experience?.toInt(),
            coins = cq.coins?.toInt(),
            bounty = cq.bounty?.let {
                val cr = DbBounty(it)
                when {
                    cr.type == DbBounty.Type.NONE.name -> Quest.Bounty.None
                    cr.type == DbBounty.Type.FOOD.name -> Quest.Bounty.Food(Food.valueOf(cr.name!!))
                    else -> null
                }
            },
            completedAtDate = cq.completedAtDate?.startOfDayUTC,
            completedAtTime = cq.completedAtMinute?.let {
                Time.of(it.toInt())
            },
            reminder = cq.reminder?.let {
                val cr = DbReminder(it)
                Reminder(cr.message, Time.of(cr.minute), cr.date?.startOfDayUTC)
            },
            timeRanges = cq.timeRanges.map {
                val ctr = DbTimeRange(it)
                TimeRange(
                    TimeRange.Type.valueOf(ctr.type),
                    ctr.duration,
                    ctr.start?.instant,
                    ctr.end?.instant
                )
            },
            repeatingQuestId = cq.repeatingQuestId,
            challengeId = cq.challengeId
        )
    }

    override fun toDatabaseObject(entity: Quest): DbQuest {
        val q = DbQuest()
        q.id = entity.id
        q.name = entity.name
        q.category = entity.category.name
        q.color = entity.color.name
        q.icon = entity.icon?.name
        q.duration = entity.duration
        q.scheduledDate = entity.scheduledDate.startOfDayUTC()
        q.originalScheduledDate = entity.originalScheduledDate.startOfDayUTC()
        q.reminder = entity.reminder?.let {
            createDbReminder(it).map
        }
        q.experience = entity.experience?.toLong()
        q.coins = entity.coins?.toLong()
        q.bounty = entity.bounty?.let {
            val cr = DbBounty()

            cr.type = when (it) {
                Quest.Bounty.None -> DbBounty.Type.NONE.name
                is Quest.Bounty.Food -> DbBounty.Type.FOOD.name
            }

            if (it is Quest.Bounty.Food) {
                cr.name = it.food.name
            }

            cr.map
        }
        q.startMinute = entity.startTime?.toMinuteOfDay()?.toLong()
        q.completedAtDate = entity.completedAtDate?.startOfDayUTC()
        q.completedAtMinute = entity.completedAtTime?.toMinuteOfDay()?.toLong()
        q.timeRanges = entity.timeRanges.map {
            createDbTimeRange(it).map
        }
        q.timeRangeCount = q.timeRanges.size
        q.repeatingQuestId = entity.repeatingQuestId
        q.challengeId = entity.challengeId
        return q
    }

    private fun createDbTimeRange(timeRange: TimeRange): DbTimeRange {
        val cTimeRange = DbTimeRange()
        cTimeRange.type = timeRange.type.name
        cTimeRange.duration = timeRange.duration
        cTimeRange.start = timeRange.start?.toEpochMilli()
        cTimeRange.end = timeRange.end?.toEpochMilli()
        return cTimeRange
    }

    private fun createDbReminder(reminder: Reminder): DbReminder {
        val cr = DbReminder()
        cr.message = reminder.message
        cr.date = reminder.remindDate!!.startOfDayUTC()
        cr.minute = reminder.remindTime.toMinuteOfDay()
        return cr
    }

}