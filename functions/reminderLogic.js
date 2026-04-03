const ONE_DAY_MS = 24 * 60 * 60 * 1000;
const REMINDER_LEAD_MS = 10 * 60 * 1000;
const WINDOW_PAST_MS = 90 * 1000;
const WINDOW_FUTURE_MS = 30 * 1000;
const REVISION_DAYS = [1, 2, 4, 7];

function timeOffsetMillis(hour, minute) {
  if (!Number.isInteger(hour) || !Number.isInteger(minute)) {
    return null;
  }

  if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
    return null;
  }

  return ((hour * 60) + minute) * 60 * 1000;
}

function isReminderEnabled(value) {
  return value !== false;
}

function numberSet(values) {
  return new Set(
    Array.isArray(values)
      ? values
          .map((value) => Number(value))
          .filter((value) => Number.isFinite(value))
      : []
  );
}

function isInsideReminderWindow(triggerAtMillis, dueAtMillis, nowMillis) {
  const windowStart = nowMillis - WINDOW_PAST_MS;
  const windowEnd = nowMillis + WINDOW_FUTURE_MS;
  return triggerAtMillis >= windowStart &&
    triggerAtMillis < windowEnd &&
    dueAtMillis > nowMillis;
}

function dayStartFromDueAt(dueAtMillis, hour, minute) {
  const offsetMillis = timeOffsetMillis(hour, minute);
  return offsetMillis === null ? null : dueAtMillis - offsetMillis;
}

function hasHabitProgressForDueDay(habit, dueAtMillis) {
  const dueDayStart = dayStartFromDueAt(
    dueAtMillis,
    habit.startHour,
    habit.startMinute
  );
  if (dueDayStart === null) {
    return false;
  }

  const completionDates = numberSet(habit.completionDates);
  const inProgressDates = numberSet(habit.inProgressDates);
  return completionDates.has(dueDayStart) || inProgressDates.has(dueDayStart);
}

function findHabitDueAtInWindow(habit, nowMillis) {
  if (!isReminderEnabled(habit.alarmEnabled) || !Number.isInteger(habit.startDateMillis)) {
    return null;
  }

  const offsetMillis = timeOffsetMillis(habit.startHour, habit.startMinute);
  if (offsetMillis === null) {
    return null;
  }

  const firstDueAtMillis = habit.startDateMillis + offsetMillis;
  const firstTriggerAtMillis = firstDueAtMillis - REMINDER_LEAD_MS;
  const approximateIndex = Math.floor((nowMillis - firstTriggerAtMillis) / ONE_DAY_MS);
  const startIndex = Math.max(0, approximateIndex - 1);
  const endIndex = Math.max(0, approximateIndex + 1);

  for (let index = startIndex; index <= endIndex; index += 1) {
    const triggerAtMillis = firstTriggerAtMillis + (index * ONE_DAY_MS);
    const dueAtMillis = triggerAtMillis + REMINDER_LEAD_MS;
    if (!isInsideReminderWindow(triggerAtMillis, dueAtMillis, nowMillis)) {
      continue;
    }
    if (hasHabitProgressForDueDay(habit, dueAtMillis)) {
      continue;
    }
    return dueAtMillis;
  }

  return null;
}

function findRevisionReminderInWindow(topic, nowMillis) {
  if (!isReminderEnabled(topic.alarmEnabled) || !Number.isInteger(topic.startDateMillis)) {
    return null;
  }

  const offsetMillis = timeOffsetMillis(topic.revisionHour, topic.revisionMinute);
  if (offsetMillis === null) {
    return null;
  }

  const completedDays = numberSet(topic.completedDays);
  const inProgressDays = numberSet(topic.inProgressDays);
  const nextDay = REVISION_DAYS.find((day) => !completedDays.has(day));
  if (!nextDay || inProgressDays.has(nextDay)) {
    return null;
  }

  const dueAtMillis = topic.startDateMillis + ((nextDay - 1) * ONE_DAY_MS) + offsetMillis;
  const triggerAtMillis = dueAtMillis - REMINDER_LEAD_MS;

  if (!isInsideReminderWindow(triggerAtMillis, dueAtMillis, nowMillis)) {
    return null;
  }

  return {
    day: nextDay,
    dueAtMillis,
  };
}

function findMissedRevisionDay(topic, nowMillis) {
  if (!Number.isInteger(topic.startDateMillis)) {
    return null;
  }

  const completedDays = new Set(
    Array.isArray(topic.completedDays)
      ? topic.completedDays
          .map((value) => Number(value))
          .filter((day) => REVISION_DAYS.includes(day))
      : []
  );

  for (const day of REVISION_DAYS) {
    if (completedDays.has(day)) {
      continue;
    }

    const previousDays = REVISION_DAYS.filter((previousDay) => previousDay < day);
    if (!previousDays.every((previousDay) => completedDays.has(previousDay))) {
      return null;
    }

    const dueDayStart = topic.startDateMillis + ((day - 1) * ONE_DAY_MS);
    return nowMillis >= dueDayStart + ONE_DAY_MS ? day : null;
  }

  return null;
}

function currentRevisionDayStart(startDateMillis, nowMillis) {
  if (!Number.isInteger(startDateMillis)) {
    return nowMillis;
  }

  if (nowMillis <= startDateMillis) {
    return startDateMillis;
  }

  const elapsedDays = Math.floor((nowMillis - startDateMillis) / ONE_DAY_MS);
  return startDateMillis + (elapsedDays * ONE_DAY_MS);
}

module.exports = {
  ONE_DAY_MS,
  REMINDER_LEAD_MS,
  REVISION_DAYS,
  WINDOW_FUTURE_MS,
  WINDOW_PAST_MS,
  currentRevisionDayStart,
  findHabitDueAtInWindow,
  findMissedRevisionDay,
  findRevisionReminderInWindow,
  isInsideReminderWindow,
  timeOffsetMillis,
};
