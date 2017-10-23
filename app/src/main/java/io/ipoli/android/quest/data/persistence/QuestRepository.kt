package io.ipoli.android.quest.data.persistence

import com.couchbase.lite.*
import com.couchbase.lite.Expression.property
import io.ipoli.android.common.datetime.DateUtils
import io.ipoli.android.common.datetime.Time
import io.ipoli.android.common.datetime.startOfDayUTC
import io.ipoli.android.common.persistence.PersistedModel
import io.ipoli.android.common.persistence.Repository
import io.ipoli.android.quest.*
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.launch
import org.threeten.bp.LocalDate
import timber.log.Timber
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 8/20/17.
 */

interface CouchbasePersistedModel : PersistedModel {
    var type: String
}

interface QuestRepository : Repository<Quest> {
    fun listenForScheduledBetween(startDate: LocalDate, endDate: LocalDate): ReceiveChannel<List<Quest>>
    fun listenForDate(date: LocalDate): ReceiveChannel<List<Quest>>
}

data class CouchbaseQuest(override val map: MutableMap<String, Any?> = mutableMapOf()) : CouchbasePersistedModel {
    override var type: String by map
    override var id: String by map
    var name: String by map
    var color: String by map
    var category: String by map
    var duration: Int by map
    var reminders: List<MutableMap<String, Any?>> by map
    var startMinute: Long? by map
    var scheduledDate: Long? by map
    override var createdAt: Long by map
    override var updatedAt: Long by map
    override var removedAt: Long? by map

    companion object {
        const val TYPE = "Quest"
    }
}

data class CouchbaseReminder(val map: MutableMap<String, Any?> = mutableMapOf()) {
    var notificationId: String by map
    var message: String by map
    var remindMinute: Int by map
    var remindDate: Long by map
}

abstract class BaseCouchbaseRepository<E, out T>(private val database: Database, private val coroutineContext: CoroutineContext) : Repository<E> where E : Entity, T : CouchbasePersistedModel {
    protected abstract val modelType: String

    override fun listenById(id: String) =
        listenForChange(
            where = property("id").equalTo(id)
        )

    protected fun listenForChange(where: Expression? = null, limit: Int? = null, orderBy: Ordering? = null) =
        sendLiveResult(createQuery(where, limit, orderBy))

    protected fun listenForChanges(where: Expression? = null, limit: Int? = null, orderBy: Ordering? = null) =
        sendLiveResults(createQuery(where, limit, orderBy))

    protected fun createQuery(where: Expression? = null, limit: Int? = null, orderBy: Ordering? = null): Query {
        val typeWhere = property("type").equalTo(modelType)
            .and(property("removedAt").isNullOrMissing)
        val w = if (where == null) typeWhere else typeWhere.and(where)

        val q = selectAll().where(w)
        orderBy?.let { q.orderBy(it) }
        limit?.let { q.limit(it) }
        return q
    }

    override fun listenForAll() = listenForChanges()

    protected fun sendLiveResults(query: Query): ReceiveChannel<List<E>> {
        val liveQuery = query.toLive()
        val channel = Channel<List<E>>()
        val changeListener = createChangeListener(liveQuery, channel) { changes ->
            val result = toEntities(changes)
            launch(coroutineContext) {
                Timber.d("Sending changes ${changes.rows}")
                channel.send(result.toList())
            }
        }
        runLiveQuery(liveQuery, changeListener)
        return channel
    }

    private fun toEntities(changes: LiveQueryChange): List<E> =
        toEntities(changes.rows.iterator())

    private fun toEntities(iterator: MutableIterator<Result>): List<E> {
        val list = mutableListOf<E>()
        iterator.forEach {
            list.add(toEntityObject(it))
        }
        return list
    }

    private fun sendLiveResult(query: Query): ReceiveChannel<E?> {
        val liveQuery = query.toLive()
        val channel = Channel<E?>()
        val changeListener = createChangeListener(liveQuery, channel) { changes ->
            val result = toEntities(changes)
            launch(coroutineContext) {
                channel.send(result.firstOrNull())
            }
        }
        runLiveQuery(liveQuery, changeListener)
        return channel
    }

    private fun <E> createChangeListener(
        query: LiveQuery,
        channel: SendChannel<E>,
        handler: (changes: LiveQueryChange) -> Unit
    ): LiveQueryChangeListener {
        var changeListener: LiveQueryChangeListener? = null

        changeListener = LiveQueryChangeListener { changes ->
            if (channel.isClosedForSend) {
                query.removeChangeListener(changeListener)
                query.stop()
            } else {
                handler(changes)
            }
        }
        return changeListener
    }

    private fun runLiveQuery(query: LiveQuery, changeListener: LiveQueryChangeListener) {
        query.addChangeListener(changeListener)
        query.run()
    }

    private fun runQuery(where: Expression? = null, limit: Int? = null, orderBy: Ordering? = null) =
        createQuery(where, limit, orderBy).run().iterator()

    override fun find() =
        toEntities(
            runQuery(
                limit = 1
            )
        ).firstOrNull()

    protected fun selectAll(): From =
        Query.select(SelectResult.all(), SelectResult.expression(Expression.meta().id))
            .from(DataSource.database(database))

    override fun save(entity: E): E {
        val cbObject = toCouchbaseObject(entity)

        val doc = if (cbObject.id.isNotEmpty()) {
            cbObject.updatedAt = System.currentTimeMillis()
            database.getDocument(cbObject.id)
        } else {
            Document()
        }

        val cbMap = cbObject.map.toMutableMap()
        cbMap.remove("id")
        doc.set(cbMap)
        database.save(doc)

        val docMap = doc.toMap().toMutableMap()
        docMap["id"] = doc.id

        return toEntityObject(docMap)
    }

    override fun remove(entity: E) {
        remove(entity.id)
    }

    override fun remove(id: String) {
        val doc = database.getDocument(id)
        doc.setLong("removedAt", DateUtils.nowUTC().time)
        database.save(doc)
    }

    override fun undoRemove(id: String) {
        val doc = database.getDocument(id)
        doc.remove("removedAt")
        database.save(doc)
    }

    protected fun toEntityObject(row: Result): E {
        val rowMap = row.toMap()
        @Suppress("UNCHECKED_CAST")
        val map = rowMap["iPoli"] as MutableMap<String, Any?>
        map.put("id", rowMap["_id"])
        return toEntityObject(map)
    }

    protected abstract fun toEntityObject(dataMap: MutableMap<String, Any?>): E

    protected abstract fun toCouchbaseObject(entity: E): T
}

class CouchbaseQuestRepository(database: Database, coroutineContext: CoroutineContext) : BaseCouchbaseRepository<Quest, CouchbaseQuest>(database, coroutineContext), QuestRepository {
    override val modelType = CouchbaseQuest.TYPE

    override fun listenForScheduledBetween(startDate: LocalDate, endDate: LocalDate) =
        listenForChanges(
            property("scheduledDate")
                .between(startDate.startOfDayUTC(), endDate.startOfDayUTC())
        )

    override fun listenForDate(date: LocalDate) =
        listenForChanges(property("scheduledDate").equalTo(date.startOfDayUTC()))

    override fun toEntityObject(dataMap: MutableMap<String, Any?>): Quest {
        val cq = CouchbaseQuest(dataMap)

        val reminders = cq.reminders
            .map {
                CouchbaseReminder(it)
            }
            .map {
                Reminder(
                    notificationId = it.notificationId,
                    message = it.message,
                    remindTime = Time.of(it.remindMinute),
                    remindDate = DateUtils.fromMillis(it.remindDate)
                )
            }

        val plannedDate = cq.scheduledDate?.let { DateUtils.fromMillis(it) }
        val plannedTime = cq.startMinute?.let { Time.of(it.toInt()) }

        return Quest(
            id = cq.id,
            name = cq.name,
            color = Color.valueOf(cq.color),
            category = Category(cq.category, Color.GREEN),
            plannedSchedule = QuestSchedule(plannedDate, plannedTime, cq.duration),
            reminders = reminders
        )
    }

    override fun toCouchbaseObject(entity: Quest): CouchbaseQuest {
        val q = CouchbaseQuest()
        q.id = entity.id
        q.name = entity.name
        q.category = entity.category.name
        q.color = entity.color.name
        q.duration = entity.plannedSchedule.duration
        q.type = CouchbaseQuest.TYPE
        q.scheduledDate = DateUtils.toMillis(entity.plannedSchedule.date!!)
        q.reminders = entity.reminders.map { r ->
            createCouchbaseReminder(r).map
        }
        entity.plannedSchedule.time?.let { q.startMinute = it.toMinuteOfDay().toLong() }
        return q
    }

    private fun createCouchbaseReminder(reminder: Reminder): CouchbaseReminder {
        val cr = CouchbaseReminder()
        cr.notificationId = reminder.notificationId
        cr.message = reminder.message
        cr.remindDate = reminder.remindDate.startOfDayUTC()
        cr.remindMinute = reminder.remindTime.toMinuteOfDay()
        return cr
    }
}