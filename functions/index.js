const { logger } = require("firebase-functions");
const { onSchedule } = require("firebase-functions/scheduler");
const { initializeApp } = require("firebase-admin/app");
const { FieldValue, getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const {
  currentRevisionDayStart,
  findHabitDueAtInWindow,
  findMissedRevisionDay,
  findRevisionReminderInWindow,
} = require("./reminderLogic");

initializeApp();

const db = getFirestore();

const USERS_COLLECTION = "users";
const TOKENS_COLLECTION = "notificationTokens";
const DISPATCHES_COLLECTION = "reminderDispatches";
const INVALID_TOKEN_CODES = new Set([
  "messaging/invalid-registration-token",
  "messaging/registration-token-not-registered",
]);

exports.sendReminderNotifications = onSchedule("* * * * *", async () => {
  const nowMillis = Date.now();
  const [habitSnapshot, revisionSnapshot] = await Promise.all([
    db.collectionGroup("habits")
      .where("startDateMillis", "<=", nowMillis)
      .get(),
    db.collectionGroup("revisionTopics")
      .where("startDateMillis", "<=", nowMillis)
      .get(),
  ]);

  let habitDispatches = 0;
  let revisionDispatches = 0;

  for (const document of habitSnapshot.docs) {
    const userId = getUserId(document.ref);
    if (!userId) {
      continue;
    }

    const data = document.data();
    const dueAtMillis = findHabitDueAtInWindow(data, nowMillis);
    if (!dueAtMillis) {
      continue;
    }

    const dispatched = await sendReminderIfPossible({
      userId,
      dispatchId: `habit_${document.id}_${dueAtMillis}`,
      title: "Habit in 10 minutes",
      body: `${safeName(data.name, "A habit")} starts in 10 minutes.`,
      payload: {
        reminderType: "habit",
        itemId: document.id,
        dueAtMillis: String(dueAtMillis),
      },
      dueAtMillis,
    });

    if (dispatched) {
      habitDispatches += 1;
    }
  }

  for (const document of revisionSnapshot.docs) {
    const userId = getUserId(document.ref);
    if (!userId) {
      continue;
    }

    const data = await normalizeRevisionTopic(document.ref, document.data(), nowMillis);
    const reminder = findRevisionReminderInWindow(data, nowMillis);
    if (!reminder) {
      continue;
    }

    const dispatched = await sendReminderIfPossible({
      userId,
      dispatchId: `revision_${document.id}_${reminder.day}_${reminder.dueAtMillis}`,
      title: "Revision in 10 minutes",
      body: `Day ${reminder.day} for ${safeName(data.name, "your topic")} starts in 10 minutes.`,
      payload: {
        reminderType: "revision",
        itemId: document.id,
        revisionDay: String(reminder.day),
        dueAtMillis: String(reminder.dueAtMillis),
      },
      dueAtMillis: reminder.dueAtMillis,
    });

    if (dispatched) {
      revisionDispatches += 1;
    }
  }

  logger.info("Reminder push run finished", {
    habitDispatches,
    revisionDispatches,
    habitDocumentsScanned: habitSnapshot.size,
    revisionDocumentsScanned: revisionSnapshot.size,
  });
});

function getUserId(documentRef) {
  return documentRef.parent.parent ? documentRef.parent.parent.id : null;
}

function safeName(value, fallback) {
  return typeof value === "string" && value.trim() ? value.trim() : fallback;
}

async function normalizeRevisionTopic(documentRef, topic, nowMillis) {
  const missedDay = findMissedRevisionDay(topic, nowMillis);
  if (!missedDay) {
    return topic;
  }

  const restartStartDateMillis = currentRevisionDayStart(topic.startDateMillis, nowMillis);
  await documentRef.set({
    startDateMillis: restartStartDateMillis,
    completedDays: [],
    inProgressDays: [],
    completionDialogDismissed: false,
  }, { merge: true });

  logger.debug("Restarted revision topic after missed day", {
    topicId: documentRef.id,
    missedDay,
    restartStartDateMillis,
  });

  return {
    ...topic,
    startDateMillis: restartStartDateMillis,
    completedDays: [],
    inProgressDays: [],
    completionDialogDismissed: false,
  };
}

async function sendReminderIfPossible({
  userId,
  dispatchId,
  title,
  body,
  payload,
  dueAtMillis,
}) {
  const userRef = db.collection(USERS_COLLECTION).doc(userId);
  const tokenSnapshot = await userRef.collection(TOKENS_COLLECTION).get();
  if (tokenSnapshot.empty) {
    return false;
  }

  const tokenEntries = dedupeTokenEntries(tokenSnapshot.docs);
  if (tokenEntries.length === 0) {
    return false;
  }

  const dispatchRef = userRef.collection(DISPATCHES_COLLECTION).doc(dispatchId);

  try {
    await dispatchRef.create({
      reminderType: payload.reminderType,
      itemId: payload.itemId,
      dueAtMillis,
      status: "sending",
      createdAt: FieldValue.serverTimestamp(),
    });
  } catch (error) {
    if (isAlreadyExistsError(error)) {
      return false;
    }
    throw error;
  }

  try {
    const { successCount, failureCount } = await sendToTokens({
      tokenEntries,
      title,
      body,
      payload,
      dispatchId,
    });

    await dispatchRef.set({
      title,
      body,
      status: "sent",
      sentAt: FieldValue.serverTimestamp(),
      successCount,
      failureCount,
    }, { merge: true });

    return successCount > 0 || failureCount > 0;
  } catch (error) {
    await dispatchRef.delete().catch(() => {});
    logger.error("Failed to send reminder push", {
      userId,
      dispatchId,
      error: error.message,
    });
    return false;
  }
}

async function sendToTokens({
  tokenEntries,
  title,
  body,
  payload,
  dispatchId,
}) {
  let successCount = 0;
  let failureCount = 0;

  for (const chunk of chunkArray(tokenEntries, 500)) {
    const response = await getMessaging().sendEachForMulticast({
      tokens: chunk.map((entry) => entry.token),
      notification: {
        title,
        body,
      },
      data: {
        ...payload,
        title,
        body,
      },
      android: {
        priority: "high",
        notification: {
          channelId: "destiny_reminders",
          tag: dispatchId,
        },
      },
    });

    successCount += response.successCount;
    failureCount += response.failureCount;

    const invalidRefs = [];
    response.responses.forEach((sendResult, index) => {
      if (!sendResult.success && INVALID_TOKEN_CODES.has(sendResult.error?.code)) {
        invalidRefs.push(chunk[index].ref);
      }
    });

    await Promise.all(invalidRefs.map((ref) => ref.delete().catch(() => {})));
  }

  return { successCount, failureCount };
}

function dedupeTokenEntries(documents) {
  const uniqueEntries = new Map();

  for (const document of documents) {
    const token = document.get("token");
    if (typeof token !== "string" || !token.trim() || uniqueEntries.has(token)) {
      continue;
    }

    uniqueEntries.set(token, {
      token,
      ref: document.ref,
    });
  }

  return Array.from(uniqueEntries.values());
}

function chunkArray(values, chunkSize) {
  const chunks = [];
  for (let index = 0; index < values.length; index += chunkSize) {
    chunks.push(values.slice(index, index + chunkSize));
  }
  return chunks;
}

function isAlreadyExistsError(error) {
  return error?.code === 6 || error?.code === "already-exists";
}
