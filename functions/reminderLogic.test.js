const test = require("node:test");
const assert = require("node:assert/strict");

const {
  findHabitDueAtInWindow,
  findRevisionReminderInWindow,
} = require("./reminderLogic");

test("skips habit reminder when the habit is already completed for the due day", () => {
  const startDateMillis = Date.UTC(2026, 3, 3, 0, 0, 0, 0);
  const nowMillis = Date.UTC(2026, 3, 3, 9, 50, 0, 0);
  const dueAtMillis = Date.UTC(2026, 3, 3, 10, 0, 0, 0);

  const result = findHabitDueAtInWindow(
    {
      startDateMillis,
      startHour: 10,
      startMinute: 0,
      completionDates: [startDateMillis],
      inProgressDates: [],
      alarmEnabled: true,
    },
    nowMillis
  );

  assert.equal(result, null);
  assert.equal(dueAtMillis, startDateMillis + (10 * 60 * 60 * 1000));
});

test("skips habit reminder when alarms are disabled", () => {
  const nowMillis = Date.UTC(2026, 3, 3, 9, 50, 0, 0);

  const result = findHabitDueAtInWindow(
    {
      startDateMillis: Date.UTC(2026, 3, 3, 0, 0, 0, 0),
      startHour: 10,
      startMinute: 0,
      completionDates: [],
      inProgressDates: [],
      alarmEnabled: false,
    },
    nowMillis
  );

  assert.equal(result, null);
});

test("skips revision reminder when the next day is already in progress", () => {
  const startDateMillis = Date.UTC(2026, 3, 3, 0, 0, 0, 0);
  const nowMillis = Date.UTC(2026, 3, 3, 9, 50, 0, 0);

  const result = findRevisionReminderInWindow(
    {
      startDateMillis,
      revisionHour: 10,
      revisionMinute: 0,
      completedDays: [],
      inProgressDays: [1],
      alarmEnabled: true,
    },
    nowMillis
  );

  assert.equal(result, null);
});

test("returns the next revision reminder when the topic is still actionable", () => {
  const startDateMillis = Date.UTC(2026, 3, 3, 0, 0, 0, 0);
  const nowMillis = Date.UTC(2026, 3, 3, 9, 50, 0, 0);

  const result = findRevisionReminderInWindow(
    {
      startDateMillis,
      revisionHour: 10,
      revisionMinute: 0,
      completedDays: [],
      inProgressDays: [],
      alarmEnabled: true,
    },
    nowMillis
  );

  assert.deepEqual(result, {
    day: 1,
    dueAtMillis: Date.UTC(2026, 3, 3, 10, 0, 0, 0),
  });
});
