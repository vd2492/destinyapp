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

    /**
     * Base flow that auto-resets any habit with missed days.
     * If a day before today was missed, the habit restarts from today
     * (startDateMillis = today, completionDates and inProgressDates cleared).
     */
    private fun habitsFlowWithAutoReset(): Flow<List<HabitDocument>> {
        return habitsFlow().map { habits ->
            val now = Calendar.getInstance().timeInMillis
            val todayStart = todayStartMillis(now)
            val habitsCollection = currentUserHabitsCollection()

            habits.map { habit ->
                if (habit.startDateMillis >= todayStart) return@map habit

                val history = calculateHabitHistory(
                    startDateMillis = habit.startDateMillis,
                    completionDates = habit.completionDates.toSet(),
                    nowMillis = now
                )

                if (history.missedDaysCount > 0) {
                    val resetSucceeded = runCatching {
                        habitsCollection?.document(habit.id)?.update(
                            mapOf(
                                "startDateMillis" to todayStart,
                                "completionDates" to emptyList<Long>(),
                                "inProgressDates" to emptyList<Long>(),
                                "thirtyDayDialogDismissed" to false
                            )
                        )?.awaitResult()
                    }.onFailure { throwable ->
                        Log.w(TAG, "Failed to auto-reset habit ${habit.id}", throwable)
                    }.isSuccess

                    if (resetSucceeded) {
                        // Return reset data immediately so UI doesn't flicker.
                        habit.copy(
                            startDateMillis = todayStart,
                            completionDates = emptyList(),
                            inProgressDates = emptyList(),
                            thirtyDayDialogDismissed = false
                        )
                    } else {
                        habit
                    }
                } else {
                    habit
                }
            }
        }
    }

    /** All habits with today's completion status for the home screen. */
    fun getTodayHabitsWithCompletion(): Flow<List<HabitWithCompletion>> {
        return habitsFlowWithAutoReset().map { habits ->
            val now = Calendar.getInstance().timeInMillis
            val todayStart = todayStartMillis(now)
            habits
                .filter { habit -> habit.startDateMillis <= todayStart }
                .map { habit ->
                    val completionDates = habit.completionDates.toSet()
                    val inProgressDates = habit.inProgressDates.toSet()
                    val todayState = when {
                        todayStart in completionDates -> HabitCompletionState.Completed
                        todayStart in inProgressDates -> HabitCompletionState.InProgress
                        else -> HabitCompletionState.NotStarted
                    }
                    val streak = computeDisplayedHabitStreak(
                        completionDates = completionDates,
                        startDateMillis = habit.startDateMillis,
                        startHour = habit.startHour,
                        startMinute = habit.startMinute,
                        todayState = todayState,
                        todayStart = todayStart,
                        nowMillis = now
                    )

                    HabitWithCompletion(
                        id = habit.id,
                        name = habit.name,
                        state = todayState,
                        startHour = habit.startHour,
                        startMinute = habit.startMinute,
                        missedDaysCount = 0,
                        latestMissedDateMillis = null,
                        alarmEnabled = habit.alarmEnabled,
                        hasThirtyDayMilestone = streak >= 30,
                        thirtyDayDialogDismissed = habit.thirtyDayDialogDismissed
                    )
                }
        }
    }

    /** All habits with streak and 30-day completion rate for the Habits screen. */
    fun getHabitsWithStats(): Flow<List<HabitWithStats>> {
        return habitsFlowWithAutoReset().map { habits ->
            val now = Calendar.getInstance().timeInMillis
            val todayStart = todayStartMillis(now)
            habits.map { habit ->
                val completionDates = habit.completionDates.toSet()
                val inProgressDates = habit.inProgressDates.toSet()
                val todayState = when {
                    todayStart in completionDates -> HabitCompletionState.Completed
                    todayStart in inProgressDates -> HabitCompletionState.InProgress
                    else -> HabitCompletionState.NotStarted
                }
                val streak = computeDisplayedHabitStreak(
                    completionDates = completionDates,
                    startDateMillis = habit.startDateMillis,
                    startHour = habit.startHour,
                    startMinute = habit.startMinute,
                    todayState = todayState,
                    todayStart = todayStart,
                    nowMillis = now
                )
                val trackedCompletions = completionDates.count { date ->
                    date >= habit.startDateMillis && date < habit.startDateMillis + thirtyDaysMillis
                }
                val completionRate = ((trackedCompletions.toFloat() / 30) * 100).toInt().coerceIn(0, 100)

                HabitWithStats(
                    id = habit.id,
                    name = habit.name,
                    streakDays = streak,
                    completionRatePercent = completionRate,
                    startDateMillis = habit.startDateMillis,
                    startHour = habit.startHour,
                    startMinute = habit.startMinute,
                    missedDaysCount = 0,
                    latestMissedDateMillis = null,
                    todayState = todayState,
                    alarmEnabled = habit.alarmEnabled,
                    hasThirtyDayMilestone = streak >= 30,
                    thirtyDayDialogDismissed = habit.thirtyDayDialogDismissed
                )
            }
        }
    }

    private fun computeDisplayedHabitStreak(
        completionDates: Set<Long>,
        startDateMillis: Long,
        startHour: Int,
        startMinute: Int,
        todayState: HabitCompletionState,
        todayStart: Long,
        nowMillis: Long
    ): Int {
        val latestExpectedDateMillis = when (todayState) {
            HabitCompletionState.Completed -> todayStart
            HabitCompletionState.NotStarted,
            HabitCompletionState.InProgress -> latestExpectedHabitDate(
                startDateMillis = startDateMillis,
                startHour = startHour,
                startMinute = startMinute,
                nowMillis = nowMillis
            )
        }
        return computeHabitStreak(
            completionDates = completionDates,
            startDateMillis = startDateMillis,
            latestExpectedDateMillis = latestExpectedDateMillis
        )
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

    suspend fun setHabitStateToday(habitId: String, newState: HabitCompletionState) {
        val habitsCollection = currentUserHabitsCollection() ?: return
        val today = todayStartMillis()
        val updates = when (newState) {
            HabitCompletionState.InProgress -> mapOf(
                "inProgressDates" to FieldValue.arrayUnion(today),
                "completionDates" to FieldValue.arrayRemove(today)
            )
            HabitCompletionState.Completed -> mapOf(
                "completionDates" to FieldValue.arrayUnion(today),
                "inProgressDates" to FieldValue.arrayRemove(today)
            )
            HabitCompletionState.NotStarted -> mapOf(
                "completionDates" to FieldValue.arrayRemove(today),
                "inProgressDates" to FieldValue.arrayRemove(today)
            )
        }
        habitsCollection.document(habitId).update(updates).awaitResult()
    }

    suspend fun addHabit(
        name: String,
        startDateMillis: Long,
        startHour: Int,
        startMinute: Int
    ): String {
        val habitsCollection = currentUserHabitsCollection() ?: return ""
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return ""
        val id = UUID.randomUUID().toString()
        val entity = HabitDocument(
            id = id,
            name = trimmedName,
            createdAtMillis = Calendar.getInstance().timeInMillis,
            startDateMillis = startDateMillis,
            startHour = startHour.coerceIn(0, 23),
            startMinute = startMinute.coerceIn(0, 59),
            completionDates = emptyList()
        )
        habitsCollection.document(id).set(entity).awaitResult()
        return id
    }

    suspend fun toggleHabitAlarm(habitId: String, enabled: Boolean) {
        currentUserHabitsCollection()
            ?.document(habitId)
            ?.update("alarmEnabled", enabled)
            ?.awaitResult()
    }

    suspend fun acknowledgeHabitThirtyDayDialog(habitId: String) {
        currentUserHabitsCollection()
            ?.document(habitId)
            ?.update("thirtyDayDialogDismissed", true)
            ?.awaitResult()
    }

    suspend fun restartHabit(habitId: String) {
        val todayStart = todayStartMillis()
        currentUserHabitsCollection()
            ?.document(habitId)
            ?.update(
                mapOf(
                    "startDateMillis" to todayStart,
                    "completionDates" to emptyList<Long>(),
                    "inProgressDates" to emptyList<Long>(),
                    "thirtyDayDialogDismissed" to false
                )
            )
            ?.awaitResult()
    }

    suspend fun toggleRevisionAlarm(topicId: String, enabled: Boolean) {
        currentUserRevisionsCollection()
            ?.document(topicId)
            ?.update("alarmEnabled", enabled)
            ?.awaitResult()
    }

    suspend fun acknowledgeRevisionCompletionDialog(topicId: String) {
        currentUserRevisionsCollection()
            ?.document(topicId)
            ?.update("completionDialogDismissed", true)
            ?.awaitResult()
    }

    suspend fun restartRevisionTopic(topicId: String) {
        val todayStart = todayStartMillis()
        currentUserRevisionsCollection()
            ?.document(topicId)
            ?.update(
                mapOf(
                    "startDateMillis" to todayStart,
                    "completedDays" to emptyList<Long>(),
                    "inProgressDays" to emptyList<Long>(),
                    "completionDialogDismissed" to false
                )
            )
            ?.awaitResult()
    }

    suspend fun deleteHabit(habitId: String) {
        currentUserHabitsCollection()
            ?.document(habitId)
            ?.delete()
            ?.awaitResult()
    }

    private fun revisionsFlowWithAutoReset(): Flow<List<RevisionTopicDocument>> {
        return revisionsFlow().map { topics ->
            val now = Calendar.getInstance().timeInMillis
            val revisionsCollection = currentUserRevisionsCollection()

            topics.map { topic ->
                autoResetRevisionTopicIfMissed(
                    topic = topic,
                    nowMillis = now,
                    revisionsCollection = revisionsCollection
                )
            }
        }
    }

    fun getRevisionTopicsWithProgress(): Flow<List<RevisionTopicWithProgress>> {
        return revisionsFlowWithAutoReset().map { topics ->
            val now = Calendar.getInstance().timeInMillis
            topics.map { topic ->
                val completedDays = revisionDaySet(topic.completedDays)
                val inProgressDays = revisionDaySet(topic.inProgressDays)
                val dayStates = buildRevisionDayStates(
                    startDateMillis = topic.startDateMillis,
                    revisionHour = topic.revisionHour,
                    revisionMinute = topic.revisionMinute,
                    completedDays = completedDays,
                    inProgressDays = inProgressDays,
                    nowMillis = now
                )
                RevisionTopicWithProgress(
                    id = topic.id,
                    name = topic.name,
                    dayStates = dayStates,
                    startDateMillis = topic.startDateMillis,
                    revisionHour = topic.revisionHour,
                    revisionMinute = topic.revisionMinute,
                    alarmEnabled = topic.alarmEnabled,
                    completionDialogDismissed = topic.completionDialogDismissed
                )
            }
        }
    }

    private fun buildRevisionDayStates(
        startDateMillis: Long,
        revisionHour: Int,
        revisionMinute: Int,
        completedDays: Set<Int>,
        inProgressDays: Set<Int>,
        nowMillis: Long
    ): List<RevisionDayProgress> {
        val todayStart = todayStartMillis(nowMillis)
        return revisionScheduleDays.map { day ->
            val previousDays = revisionScheduleDays.takeWhile { it < day }
            val dueDateMillis = calculateRevisionDueAt(
                startDateMillis = startDateMillis,
                revisionHour = revisionHour,
                revisionMinute = revisionMinute,
                revisionDay = day
            )
            val dueDayStart = todayStartMillis(dueDateMillis)
            val state = when {
                completedDays.contains(day) -> RevisionDayState.Completed
                inProgressDays.contains(day) -> RevisionDayState.InProgress
                previousDays.any { it !in completedDays } -> RevisionDayState.Locked
                todayStart in dueDayStart until (dueDayStart + oneDayMillis) -> RevisionDayState.Active
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
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return ""
        val id = UUID.randomUUID().toString()
        val entity = RevisionTopicDocument(
            id = id,
            name = trimmedName,
            createdAtMillis = Calendar.getInstance().timeInMillis,
            startDateMillis = startDateMillis,
            revisionHour = revisionHour.coerceIn(0, 23),
            revisionMinute = revisionMinute.coerceIn(0, 59),
            completedDays = emptyList()
        )
        revisionsCollection.document(id).set(entity).awaitResult()
        return id
    }

    suspend fun startRevision(topicId: String): Int? {
        val revisionsCollection = currentUserRevisionsCollection() ?: return null
        val now = Calendar.getInstance().timeInMillis
        val topic = getRevisionTopicForToday(
            topicId = topicId,
            revisionsCollection = revisionsCollection,
            nowMillis = now
        ) ?: return null
        val completedDays = revisionDaySet(topic.completedDays)
        val inProgressDays = revisionDaySet(topic.inProgressDays)
        val todayStart = todayStartMillis(now)

        val nextActiveDay = revisionScheduleDays.firstOrNull { day ->
            val previousDays = revisionScheduleDays.takeWhile { it < day }
            val dueDateMillis = calculateRevisionDueAt(
                startDateMillis = topic.startDateMillis,
                revisionHour = topic.revisionHour,
                revisionMinute = topic.revisionMinute,
                revisionDay = day
            )
            val dueDayStart = todayStartMillis(dueDateMillis)
            day !in completedDays &&
                day !in inProgressDays &&
                previousDays.all { it in completedDays } &&
                todayStart in dueDayStart until (dueDayStart + oneDayMillis)
        } ?: return null

        revisionsCollection.document(topicId)
            .update("inProgressDays", FieldValue.arrayUnion(nextActiveDay.toLong()))
            .awaitResult()
        return nextActiveDay
    }

    suspend fun completeActiveRevision(topicId: String): Int? {
        val revisionsCollection = currentUserRevisionsCollection() ?: return null
        val topic = getRevisionTopicForToday(
            topicId = topicId,
            revisionsCollection = revisionsCollection,
            nowMillis = Calendar.getInstance().timeInMillis
        ) ?: return null
        val inProgressDays = revisionDaySet(topic.inProgressDays)

        val dayToComplete = revisionScheduleDays.firstOrNull { it in inProgressDays }
            ?: return null

        revisionsCollection.document(topicId)
            .update(
                mapOf(
                    "completedDays" to FieldValue.arrayUnion(dayToComplete.toLong()),
                    "inProgressDays" to FieldValue.arrayRemove(dayToComplete.toLong())
                )
            )
            .awaitResult()
        return dayToComplete
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

    private suspend fun getRevisionTopicForToday(
        topicId: String,
        revisionsCollection: com.google.firebase.firestore.CollectionReference,
        nowMillis: Long
    ): RevisionTopicDocument? {
        val topicSnapshot = revisionsCollection.document(topicId).get().awaitResult()
        val topic = topicSnapshot.toRevisionTopicDocument() ?: return null
        return autoResetRevisionTopicIfMissed(
            topic = topic,
            nowMillis = nowMillis,
            revisionsCollection = revisionsCollection
        )
    }

    private suspend fun autoResetRevisionTopicIfMissed(
        topic: RevisionTopicDocument,
        nowMillis: Long,
        revisionsCollection: com.google.firebase.firestore.CollectionReference?
    ): RevisionTopicDocument {
        val missedDay = findMissedRevisionDay(
            startDateMillis = topic.startDateMillis,
            revisionHour = topic.revisionHour,
            revisionMinute = topic.revisionMinute,
            completedDays = revisionDaySet(topic.completedDays),
            nowMillis = nowMillis
        ) ?: return topic

        val todayStart = todayStartMillis(nowMillis)
        val resetSucceeded = runCatching {
            revisionsCollection?.document(topic.id)?.update(
                mapOf(
                    "startDateMillis" to todayStart,
                    "completedDays" to emptyList<Long>(),
                    "inProgressDays" to emptyList<Long>(),
                    "completionDialogDismissed" to false
                )
            )?.awaitResult()
        }.onFailure { throwable ->
            Log.w(TAG, "Failed to auto-reset revision ${topic.id}", throwable)
        }.isSuccess

        if (!resetSucceeded) {
            return topic
        }

        Log.d(TAG, "Revision ${topic.id} missed day $missedDay; restarting from today")
        return topic.copy(
            startDateMillis = todayStart,
            completedDays = emptyList(),
            inProgressDays = emptyList(),
            completionDialogDismissed = false
        )
    }

    private fun findMissedRevisionDay(
        startDateMillis: Long,
        revisionHour: Int,
        revisionMinute: Int,
        completedDays: Set<Int>,
        nowMillis: Long
    ): Int? {
        val todayStart = todayStartMillis(nowMillis)

        for (day in revisionScheduleDays) {
            if (day in completedDays) continue

            val previousDays = revisionScheduleDays.takeWhile { it < day }
            if (previousDays.any { it !in completedDays }) {
                return null
            }

            val dueAtMillis = calculateRevisionDueAt(
                startDateMillis = startDateMillis,
                revisionHour = revisionHour,
                revisionMinute = revisionMinute,
                revisionDay = day
            )
            val dueDayStart = todayStartMillis(dueAtMillis)
            return day.takeIf { todayStart >= dueDayStart + oneDayMillis }
        }

        return null
    }

    private fun revisionDaySet(days: List<Long>): Set<Int> {
        return days
            .map { it.toInt() }
            .filter { it in revisionScheduleDays }
            .toSet()
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
        val completionDates: List<Long> = emptyList(),
        val inProgressDates: List<Long> = emptyList(),
        val alarmEnabled: Boolean = true,
        val thirtyDayDialogDismissed: Boolean = false
    )

    private data class RevisionTopicDocument(
        val id: String = "",
        val name: String = "",
        val createdAtMillis: Long = 0L,
        val startDateMillis: Long = 0L,
        val revisionHour: Int = 0,
        val revisionMinute: Int = 0,
        val completedDays: List<Long> = emptyList(),
        val inProgressDays: List<Long> = emptyList(),
        val alarmEnabled: Boolean = true,
        val completionDialogDismissed: Boolean = false
    )

    private companion object {
        const val TAG = "HabitRepository"
        const val USERS_COLLECTION = "users"
        const val HABITS_COLLECTION = "habits"
        const val REVISIONS_COLLECTION = "revisionTopics"
    }
}

enum class HabitCompletionState {
    NotStarted,
    InProgress,
    Completed
}

data class HabitWithCompletion(
    val id: String,
    val name: String,
    val state: HabitCompletionState,
    val startHour: Int,
    val startMinute: Int,
    val missedDaysCount: Int = 0,
    val latestMissedDateMillis: Long? = null,
    val alarmEnabled: Boolean = true,
    val hasThirtyDayMilestone: Boolean = false,
    val thirtyDayDialogDismissed: Boolean = false
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
    val latestMissedDateMillis: Long? = null,
    val todayState: HabitCompletionState = HabitCompletionState.NotStarted,
    val alarmEnabled: Boolean = true,
    val hasThirtyDayMilestone: Boolean = false,
    val thirtyDayDialogDismissed: Boolean = false
)

enum class RevisionDayState {
    Completed,
    InProgress,
    Active,
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
    val revisionMinute: Int,
    val alarmEnabled: Boolean = true,
    val completionDialogDismissed: Boolean = false
) {
    val activeDay: Int?
        get() = dayStates.firstOrNull { it.state == RevisionDayState.Active }?.day

    val inProgressDay: Int?
        get() = dayStates.firstOrNull { it.state == RevisionDayState.InProgress }?.day

    val actionableDay: Int?
        get() = inProgressDay ?: activeDay

    val requiresAttention: Boolean
        get() = actionableDay != null

    val isCompleted: Boolean
        get() = dayStates.all { it.state == RevisionDayState.Completed }
}

private data class HabitHistory(
    val missedDaysCount: Int = 0,
    val latestMissedDateMillis: Long? = null
)
