package com.vishruthdev.destiny.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.vishruthdev.destiny.FirebaseRuntimeConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

class HabitRepository(
    private val firebaseConfig: FirebaseRuntimeConfig,
    private val firebaseAuth: FirebaseAuth?,
    private val firestore: FirebaseFirestore?
) {

    fun todayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private val oneDayMillis = 24 * 60 * 60 * 1000L
    private val thirtyDaysMillis = 30 * oneDayMillis
    private val revisionScheduleDays = listOf(1, 2, 4, 7)

    /** All habits with today's completion status for the home screen. */
    fun getTodayHabitsWithCompletion(): Flow<List<HabitWithCompletion>> {
        return habitsFlow().map { habits ->
            val now = Calendar.getInstance().timeInMillis
            val todayStart = todayStartMillis(now)
            habits
                .filter { habit -> habit.startDateMillis <= todayStart }
                .map { habit ->
                    val completionDates = habit.completionDates.toSet()
                    val history = calculateHabitHistory(
                        startDateMillis = habit.startDateMillis,
                        completionDates = completionDates,
                        nowMillis = now
                    )

                    HabitWithCompletion(
                        id = habit.id,
                        name = habit.name,
                        completedToday = todayStart in completionDates,
                        startHour = habit.startHour,
                        startMinute = habit.startMinute,
                        missedDaysCount = history.missedDaysCount,
                        latestMissedDateMillis = history.latestMissedDateMillis
                    )
                }
        }
    }

    /** All habits with streak and 30-day completion rate for the Habits screen. */
    fun getHabitsWithStats(): Flow<List<HabitWithStats>> {
        return habitsFlow().map { habits ->
            val now = Calendar.getInstance().timeInMillis
            habits.map { habit ->
                val completionDates = habit.completionDates.toSet()
                val streak = computeHabitStreak(
                    completionDates = completionDates,
                    startDateMillis = habit.startDateMillis,
                    latestExpectedDateMillis = latestExpectedHabitDate(
                        startDateMillis = habit.startDateMillis,
                        startHour = habit.startHour,
                        startMinute = habit.startMinute,
                        nowMillis = now
                    )
                )
                val trackedCompletions = completionDates.count { date ->
                    date >= habit.startDateMillis && date < habit.startDateMillis + thirtyDaysMillis
                }
                val completionRate = ((trackedCompletions.toFloat() / 30) * 100).toInt().coerceIn(0, 100)
                val history = calculateHabitHistory(
                    startDateMillis = habit.startDateMillis,
                    completionDates = completionDates,
                    nowMillis = now
                )

                HabitWithStats(
                    id = habit.id,
                    name = habit.name,
                    streakDays = streak,
                    completionRatePercent = completionRate,
                    startDateMillis = habit.startDateMillis,
                    startHour = habit.startHour,
                    startMinute = habit.startMinute,
                    missedDaysCount = history.missedDaysCount,
                    latestMissedDateMillis = history.latestMissedDateMillis
                )
            }
        }
    }

    private fun computeHabitStreak(
        completionDates: Set<Long>,
        startDateMillis: Long,
        latestExpectedDateMillis: Long?
    ): Int {
        val expectedDate = latestExpectedDateMillis ?: return 0
        var streak = 0
        var currentDay = expectedDate

        while (currentDay >= startDateMillis && currentDay in completionDates) {
            streak++
            currentDay -= oneDayMillis
        }

        return streak
    }

    suspend fun setCompletedToday(habitId: String, completed: Boolean) {
        val habitsCollection = currentUserHabitsCollection() ?: return
        val today = todayStartMillis()
        val operation = if (completed) {
            FieldValue.arrayUnion(today)
        } else {
            FieldValue.arrayRemove(today)
        }
        habitsCollection.document(habitId)
            .update("completionDates", operation)
            .awaitResult()
    }

    suspend fun addHabit(
        name: String,
        startDateMillis: Long,
        startHour: Int,
        startMinute: Int
    ): String {
        val habitsCollection = currentUserHabitsCollection() ?: return ""
        val id = UUID.randomUUID().toString()
        val entity = HabitDocument(
            id = id,
            name = name,
            createdAtMillis = Calendar.getInstance().timeInMillis,
            startDateMillis = startDateMillis,
            startHour = startHour.coerceIn(0, 23),
            startMinute = startMinute.coerceIn(0, 59),
            completionDates = emptyList()
        )
        habitsCollection.document(id).set(entity).awaitResult()
        return id
    }

    suspend fun deleteHabit(habitId: String) {
        currentUserHabitsCollection()
            ?.document(habitId)
            ?.delete()
            ?.awaitResult()
    }

    fun getRevisionTopicsWithProgress(): Flow<List<RevisionTopicWithProgress>> {
        return revisionsFlow().map { topics ->
            val now = Calendar.getInstance().timeInMillis
            topics.map { topic ->
                val completedDays = topic.completedDays.map { it.toInt() }.toSet()
                val dayStates = buildRevisionDayStates(
                    startDateMillis = topic.startDateMillis,
                    revisionHour = topic.revisionHour,
                    revisionMinute = topic.revisionMinute,
                    completedDays = completedDays,
                    nowMillis = now
                )
                RevisionTopicWithProgress(
                    id = topic.id,
                    name = topic.name,
                    dayStates = dayStates,
                    startDateMillis = topic.startDateMillis,
                    revisionHour = topic.revisionHour,
                    revisionMinute = topic.revisionMinute
                )
            }
        }
    }

    private fun buildRevisionDayStates(
        startDateMillis: Long,
        revisionHour: Int,
        revisionMinute: Int,
        completedDays: Set<Int>,
        nowMillis: Long
    ): List<RevisionDayProgress> {
        return revisionScheduleDays.map { day ->
            val previousDays = revisionScheduleDays.takeWhile { it < day }
            val dueDateMillis = calculateRevisionDueAt(
                startDateMillis = startDateMillis,
                revisionHour = revisionHour,
                revisionMinute = revisionMinute,
                revisionDay = day
            )
            val overdueAtMillis = todayStartMillis(dueDateMillis) + oneDayMillis
            val state = when {
                completedDays.contains(day) -> RevisionDayState.Completed
                previousDays.any { it !in completedDays } -> RevisionDayState.Locked
                nowMillis >= overdueAtMillis -> RevisionDayState.Overdue
                nowMillis >= dueDateMillis -> RevisionDayState.Active
                else -> RevisionDayState.Locked
            }
            RevisionDayProgress(day = day, state = state)
        }
    }

    private fun calculateHabitHistory(
        startDateMillis: Long,
        completionDates: Set<Long>,
        nowMillis: Long
    ): HabitHistory {
        val todayStart = todayStartMillis(nowMillis)
        val lastCompletedDayThatCanBeMissed = todayStart - oneDayMillis
        if (lastCompletedDayThatCanBeMissed < startDateMillis) {
            return HabitHistory()
        }

        var currentDay = startDateMillis
        var missedDaysCount = 0
        var latestMissedDateMillis: Long? = null

        while (currentDay <= lastCompletedDayThatCanBeMissed) {
            if (currentDay !in completionDates) {
                missedDaysCount++
                latestMissedDateMillis = currentDay
            }
            currentDay += oneDayMillis
        }

        return HabitHistory(
            missedDaysCount = missedDaysCount,
            latestMissedDateMillis = latestMissedDateMillis
        )
    }

    private fun latestExpectedHabitDate(
        startDateMillis: Long,
        startHour: Int,
        startMinute: Int,
        nowMillis: Long
    ): Long? {
        val todayStart = todayStartMillis(nowMillis)
        if (todayStart < startDateMillis) return null

        val todayDueAtMillis = calculateHabitDueAt(todayStart, startHour, startMinute)
        val latestExpectedDay = if (nowMillis >= todayDueAtMillis) {
            todayStart
        } else {
            todayStart - oneDayMillis
        }

        return latestExpectedDay.takeIf { it >= startDateMillis }
    }

    private fun calculateHabitDueAt(
        dayStartMillis: Long,
        startHour: Int,
        startMinute: Int
    ): Long {
        val timeOffsetMillis = ((startHour * 60L) + startMinute) * 60 * 1000L
        return dayStartMillis + timeOffsetMillis
    }

    private fun todayStartMillis(timeMillis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun calculateRevisionDueAt(
        startDateMillis: Long,
        revisionHour: Int,
        revisionMinute: Int,
        revisionDay: Int
    ): Long {
        val dayOffsetMillis = (revisionDay - 1).coerceAtLeast(0) * oneDayMillis
        val timeOffsetMillis = ((revisionHour * 60L) + revisionMinute) * 60 * 1000L
        return startDateMillis + dayOffsetMillis + timeOffsetMillis
    }

    suspend fun addRevisionTopic(
        name: String,
        startDateMillis: Long,
        revisionHour: Int,
        revisionMinute: Int
    ): String {
        val revisionsCollection = currentUserRevisionsCollection() ?: return ""
        val id = UUID.randomUUID().toString()
        val entity = RevisionTopicDocument(
            id = id,
            name = name,
            createdAtMillis = Calendar.getInstance().timeInMillis,
            startDateMillis = startDateMillis,
            revisionHour = revisionHour.coerceIn(0, 23),
            revisionMinute = revisionMinute.coerceIn(0, 59),
            completedDays = emptyList()
        )
        revisionsCollection.document(id).set(entity).awaitResult()
        return id
    }

    suspend fun completeActiveRevision(topicId: String): Int? {
        val revisionsCollection = currentUserRevisionsCollection() ?: return null
        val topicSnapshot = revisionsCollection.document(topicId).get().awaitResult()
        val topic = topicSnapshot.toRevisionTopicDocument() ?: return null
        val completedDays = topic.completedDays.map { it.toInt() }.toSet()
        val now = Calendar.getInstance().timeInMillis

        val nextActiveDay = revisionScheduleDays.firstOrNull { day ->
            val previousDays = revisionScheduleDays.takeWhile { it < day }
            val dueDateMillis = calculateRevisionDueAt(
                startDateMillis = topic.startDateMillis,
                revisionHour = topic.revisionHour,
                revisionMinute = topic.revisionMinute,
                revisionDay = day
            )
            day !in completedDays &&
                previousDays.all { it in completedDays } &&
                now >= dueDateMillis
        } ?: return null

        revisionsCollection.document(topicId)
            .update("completedDays", FieldValue.arrayUnion(nextActiveDay.toLong()))
            .awaitResult()
        return nextActiveDay
    }

    suspend fun resetRevisionTopicFromToday(topicId: String): Boolean {
        val revisionsCollection = currentUserRevisionsCollection() ?: return false
        val topicSnapshot = revisionsCollection.document(topicId).get().awaitResult()
        val topic = topicSnapshot.toRevisionTopicDocument() ?: return false
        revisionsCollection.document(topicId)
            .set(
                topic.copy(
                    startDateMillis = todayStartMillis(),
                    completedDays = emptyList()
                )
            )
            .awaitResult()
        return true
    }

    suspend fun deleteRevisionTopic(topicId: String) {
        currentUserRevisionsCollection()
            ?.document(topicId)
            ?.delete()
            ?.awaitResult()
    }

    suspend fun clearAll() {
        deleteAllDocuments(currentUserHabitsCollection())
        deleteAllDocuments(currentUserRevisionsCollection())
    }

    private suspend fun deleteAllDocuments(collection: com.google.firebase.firestore.CollectionReference?) {
        if (collection == null) return
        val snapshot = collection.get().awaitResult()
        if (snapshot.isEmpty) return
        val batch = firestore?.batch() ?: return
        snapshot.documents.forEach { document ->
            batch.delete(document.reference)
        }
        batch.commit().awaitResult()
    }

    private fun habitsFlow(): Flow<List<HabitDocument>> = userCollectionFlow(
        collectionName = HABITS_COLLECTION,
        mapper = { toHabitDocument() }
    )

    private fun revisionsFlow(): Flow<List<RevisionTopicDocument>> = userCollectionFlow(
        collectionName = REVISIONS_COLLECTION,
        mapper = { toRevisionTopicDocument() }
    )

    private fun <T> userCollectionFlow(
        collectionName: String,
        mapper: DocumentSnapshot.() -> T?
    ): Flow<List<T>> = callbackFlow {
        if (!firebaseConfig.isBaseConfigured || firebaseAuth == null || firestore == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        var activeUid: String? = null
        var registration: ListenerRegistration? = null
        var initialLoadJob: Job? = null

        fun clearActiveObserver() {
            registration?.remove()
            registration = null
            initialLoadJob?.cancel()
            initialLoadJob = null
        }

        val authListener = FirebaseAuth.AuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            if (uid == activeUid && registration != null) {
                return@AuthStateListener
            }

            clearActiveObserver()
            activeUid = uid

            if (uid == null) {
                trySend(emptyList())
                return@AuthStateListener
            }

            val query = userDocument(uid)
                .collection(collectionName)
                .orderBy("createdAtMillis", Query.Direction.ASCENDING)

            initialLoadJob = launch {
                runCatching {
                    query.get().awaitResult()
                }.onSuccess { snapshot ->
                    val documents = snapshot.documents.mapNotNull { document -> document.mapper() }
                    trySend(documents)
                }.onFailure { throwable ->
                    Log.w(TAG, "Initial load failed for $collectionName user=$uid", throwable)
                }
            }

            registration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listener failed for $collectionName user=$uid", error)
                    return@addSnapshotListener
                }

                val documents = snapshot?.documents
                    ?.mapNotNull { document -> document.mapper() }
                    ?: return@addSnapshotListener
                trySend(documents)
            }
        }

        firebaseAuth.addAuthStateListener(authListener)
        authListener.onAuthStateChanged(firebaseAuth)

        awaitClose {
            clearActiveObserver()
            firebaseAuth.removeAuthStateListener(authListener)
        }
    }

    private fun currentUserHabitsCollection(): com.google.firebase.firestore.CollectionReference? {
        val uid = firebaseAuth?.currentUser?.uid ?: return null
        return userDocument(uid).collection(HABITS_COLLECTION)
    }

    private fun currentUserRevisionsCollection(): com.google.firebase.firestore.CollectionReference? {
        val uid = firebaseAuth?.currentUser?.uid ?: return null
        return userDocument(uid).collection(REVISIONS_COLLECTION)
    }

    private fun userDocument(uid: String) = firestore!!
        .collection(USERS_COLLECTION)
        .document(uid)

    private fun DocumentSnapshot.toHabitDocument(): HabitDocument? {
        return toObject(HabitDocument::class.java)?.copy(id = id)
    }

    private fun DocumentSnapshot.toRevisionTopicDocument(): RevisionTopicDocument? {
        return toObject(RevisionTopicDocument::class.java)?.copy(id = id)
    }

    private data class HabitDocument(
        val id: String = "",
        val name: String = "",
        val createdAtMillis: Long = 0L,
        val startDateMillis: Long = 0L,
        val startHour: Int = 0,
        val startMinute: Int = 0,
        val completionDates: List<Long> = emptyList()
    )

    private data class RevisionTopicDocument(
        val id: String = "",
        val name: String = "",
        val createdAtMillis: Long = 0L,
        val startDateMillis: Long = 0L,
        val revisionHour: Int = 0,
        val revisionMinute: Int = 0,
        val completedDays: List<Long> = emptyList()
    )

    private companion object {
        const val TAG = "HabitRepository"
        const val USERS_COLLECTION = "users"
        const val HABITS_COLLECTION = "habits"
        const val REVISIONS_COLLECTION = "revisionTopics"
    }
}

data class HabitWithCompletion(
    val id: String,
    val name: String,
    val completedToday: Boolean,
    val startHour: Int,
    val startMinute: Int,
    val missedDaysCount: Int = 0,
    val latestMissedDateMillis: Long? = null
)

data class HabitWithStats(
    val id: String,
    val name: String,
    val streakDays: Int,
    val completionRatePercent: Int,
    val startDateMillis: Long,
    val startHour: Int,
    val startMinute: Int,
    val missedDaysCount: Int = 0,
    val latestMissedDateMillis: Long? = null
)

enum class RevisionDayState {
    Completed,
    Active,
    Overdue,
    Locked
}

data class RevisionDayProgress(
    val day: Int,
    val state: RevisionDayState
)

data class RevisionTopicWithProgress(
    val id: String,
    val name: String,
    val dayStates: List<RevisionDayProgress>,
    val startDateMillis: Long,
    val revisionHour: Int,
    val revisionMinute: Int
) {
    val activeDay: Int?
        get() = dayStates.firstOrNull { it.state == RevisionDayState.Active }?.day

    val overdueDay: Int?
        get() = dayStates.firstOrNull { it.state == RevisionDayState.Overdue }?.day

    val actionableDay: Int?
        get() = overdueDay ?: activeDay

    val requiresAttention: Boolean
        get() = actionableDay != null

    val isCompleted: Boolean
        get() = dayStates.all { it.state == RevisionDayState.Completed }
}

private data class HabitHistory(
    val missedDaysCount: Int = 0,
    val latestMissedDateMillis: Long? = null
)
