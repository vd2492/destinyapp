package com.vishruthdev.destiny.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vishruthdev.destiny.ui.theme.DestinyAccentBlue
import com.vishruthdev.destiny.ui.theme.DestinyCompletedGreen
import com.vishruthdev.destiny.ui.theme.DestinyInProgressOrange
import com.vishruthdev.destiny.ui.theme.DestinyLockedGrey
import com.vishruthdev.destiny.ui.theme.DestinyMissedRed

/**
 * A written guide to the app. Reached from Settings and from the first-login prompt; system
 * back and the arrow both return to wherever the user came from.
 *
 * Text may contain **bold** markers, which are rendered as emphasised words.
 */
@Composable
fun TutorialScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = "Tutorial",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "How to use Destiny",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        TutorialSection(title = "Quick start") {
            TutorialText(
                "Destiny has two tools. **Habits** are things you do every day. " +
                    "**Revisions** are topics you review on a schedule so you remember them."
            )
            TutorialSteps(
                "Add a habit, for example **Read 10 pages**.",
                "Each day, tap its **circle** on the Today tab when you start, and again when you finish.",
                "After you study something, add it as a **revision topic** and revise it on **Day 1, 2, 4 and 7**."
            )
        }

        TutorialSection(title = "Habits") {
            TutorialText("A habit is something you want to do every day, like \"Drink water\" or \"Read 10 pages\".")

            TutorialHeading("Add a habit")
            TutorialSteps(
                "Tap **Add Habit** on the Today tab, or **Add new Habit** on the Habits tab.",
                "Type a name, choose when it **starts** (Today, Tomorrow or Pick Date) and set the **time** you will do it.",
                "Tap **Add**."
            )

            TutorialHeading("What the circle does")
            TutorialText("On the **Today** tab, tap a habit's circle to move it along:")
            CircleLegendRow(CircleKind.NotStarted, "**Not started.** Nothing done yet today.")
            CircleLegendRow(CircleKind.InProgress, "**In progress.** You have started but not finished. This is the first tap.")
            CircleLegendRow(CircleKind.Done, "**Done.** Counts towards your streak. This is the second tap.")
            TutorialCallout(
                CalloutKind.Tip,
                "While other habits are still pending, tapping a done habit once more **undoes** it. " +
                    "When **every** habit for today is done, the list is replaced by a banner with an **Undo all** " +
                    "button for about 10 seconds, so undo straight away if you tapped by mistake."
            )

            TutorialHeading("Streak and progress")
            TutorialText(
                "Each card on the **Habits** tab shows your **streak** (days in a row) and your " +
                    "**completion rate**. A red tag such as **Missed yesterday** means you skipped a day."
            )

            TutorialHeading("Turn the alarm on or off")
            TutorialSteps(
                "Open the **Habits** tab.",
                "**Tap the habit's card.** It flips over.",
                "Use the **Alarm (2 min before)** switch. On sends a reminder 2 minutes before your habit time. Off sends none.",
                "Tap the card again to flip it back."
            )
            AlarmSwitchPreview()
            TutorialCallout(
                CalloutKind.Tip,
                "Alarms are **on by default** for every new habit. Turn them off only for the ones you don't want reminders for."
            )

            TutorialHeading("Delete a habit")
            TutorialSteps(
                "On the **Habits** tab, tap **Remove habit**. You can also **long-press** any card.",
                "Tap the **✕** on the habit you want to remove.",
                "Tap **Done** when you have finished."
            )
            TutorialCallout(
                CalloutKind.Warning,
                "Deleting is **immediate and permanent**. There is no confirmation and no undo, and the habit's streak is lost."
            )

            TutorialCallout(
                CalloutKind.Important,
                "Reach a **30-day streak** and Destiny asks whether to restart, delete or keep going. " +
                    "Flip the card and tap **Edit options** to see those choices again."
            )
        }

        TutorialSection(title = "Revisions") {
            TutorialText(
                "A revision topic is something you studied and want to remember. Reviewing it again after " +
                    "**1, 2, 4 and 7 days** helps it stick. Destiny tracks those days for you."
            )

            TutorialHeading("Add a revision topic")
            TutorialSteps(
                "Tap **Add revision topic** on the Today tab, or **Add topic** on the Revisions tab.",
                "Type a name, choose the day you **studied it** (Today, Tomorrow or Pick Date) and the **time** for your reminder.",
                "Tap **Add**."
            )
            TutorialExample(
                "Example: you study \"Photosynthesis\" on Monday. Day 1 is Monday, Day 2 is Tuesday, " +
                    "Day 4 is Thursday and Day 7 is Sunday."
            )

            TutorialHeading("Work through each day")
            TutorialSteps(
                "When a day is **Due**, tap **Start Day N**. The topic shows **In Progress**.",
                "Revise the topic.",
                "Tap **Mark Day N done** when you have finished."
            )
            TutorialCallout(
                CalloutKind.Important,
                "Later days stay **locked** until the day before is done. The tag on each card shows where it " +
                    "stands: **Planned** (not due yet), **Due**, **In Progress** or **Done**."
            )

            TutorialHeading("Turn the alarm on or off")
            TutorialSteps(
                "Open the **Revisions** tab.",
                "**Tap the topic's card.** It flips over.",
                "Use the **Alarm (2 min before)** switch to turn its reminder on or off.",
                "Tap the card again to flip it back."
            )
            AlarmSwitchPreview()

            TutorialHeading("Delete a topic")
            TutorialSteps(
                "On the **Revisions** tab, tap **Remove topic**, or **long-press** any card.",
                "Tap the **✕** on the topic you want to remove.",
                "Tap **Done** when you have finished."
            )
            TutorialCallout(
                CalloutKind.Warning,
                "Deleting is **immediate and permanent**. There is no confirmation and no undo."
            )

            TutorialCallout(
                CalloutKind.Important,
                "After **Day 7** the topic is complete and Destiny offers to restart or delete it. " +
                    "Flip the card and tap **Edit options** to see those choices again."
            )
        }

        TutorialSection(title = "Today tab") {
            TutorialText(
                "Your daily home screen. **Due Revisions** and **Due Habits** show what is left to do, " +
                    "and the ring shows how much of today's habits you have finished. " +
                    "Tap **View All >** to open the full Habits or Revisions list."
            )
        }

        TutorialSection(title = "Strict Mode") {
            TutorialText(
                "Off by default. When you turn it on in Settings, **missing a day restarts your habits and " +
                    "revisions from Day 1**. Leave it off to keep your progress when you skip a day."
            )
        }

        TutorialSection(title = "Reminders") {
            TutorialText(
                "Allow **notifications** when asked so reminders can reach you. In Settings you can also allow " +
                    "**Alarms & reminders** so they arrive exactly on time instead of a few minutes late."
            )
        }

        TutorialSection(title = "Tips for using Destiny well") {
            TutorialBullets(
                "**Start small.** Two or three habits you can keep beat ten you can't.",
                "**Set the time you really do it.** The reminder arrives 2 minutes before, so it lands at the right moment.",
                "**Tap the circle when you begin, and again when you finish.** In progress shows what is still pending.",
                "**Add a revision topic the day you study it.** Starting Today makes Day 1 today.",
                "**Never skip a revision day if you can help it.** Later days stay locked until you complete the earlier ones.",
                "**Check the Today tab** each morning and evening. It lists exactly what is due.",
                "**Missed a day?** Just carry on. With Strict Mode off your progress is kept.",
                "**Use the search box** on the Habits and Revisions tabs to find a card quickly."
            )
        }
    }
}

private enum class CalloutKind { Tip, Important, Warning }

private enum class CircleKind { NotStarted, InProgress, Done }

@Composable
private fun TutorialSection(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun TutorialHeading(text: String) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = DestinyAccentBlue
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun TutorialText(text: String) {
    Text(
        text = emphasised(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun TutorialSteps(vararg steps: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        steps.forEachIndexed { index, step ->
            Row {
                Text(
                    text = "${index + 1}.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = DestinyAccentBlue,
                    modifier = Modifier.width(24.dp)
                )
                Text(
                    text = emphasised(step),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun TutorialBullets(vararg items: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { item ->
            Row {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = DestinyAccentBlue,
                    modifier = Modifier.width(18.dp)
                )
                Text(
                    text = emphasised(item),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TutorialExample(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = emphasised(text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(12.dp)
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

/** A coloured note that pulls an important point out of the surrounding text. */
@Composable
private fun TutorialCallout(kind: CalloutKind, text: String) {
    val (label, accent) = when (kind) {
        CalloutKind.Tip -> "TIP" to DestinyAccentBlue
        CalloutKind.Important -> "IMPORTANT" to DestinyInProgressOrange
        CalloutKind.Warning -> "CAREFUL" to DestinyMissedRed
    }
    Spacer(modifier = Modifier.height(4.dp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.12f)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent)
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = accent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = emphasised(text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

/** Mirrors the real habit-circle states so the guide shows exactly what the user will see. */
@Composable
private fun CircleLegendRow(kind: CircleKind, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .then(
                    when (kind) {
                        CircleKind.Done -> Modifier.background(DestinyCompletedGreen)
                        CircleKind.InProgress -> Modifier.background(DestinyInProgressOrange)
                        CircleKind.NotStarted -> Modifier.border(2.dp, DestinyLockedGrey, CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (kind) {
                CircleKind.Done -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White
                )
                CircleKind.InProgress -> Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                CircleKind.NotStarted -> Unit
            }
        }
        Text(
            text = emphasised(text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

/** A non-interactive picture of the alarm row found on the back of a flipped card. */
@Composable
private fun AlarmSwitchPreview() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = DestinyAccentBlue
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Alarm (2 min before)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            // Display only: the real switch lives on the back of the card.
            Switch(checked = true, onCheckedChange = null)
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

/** Turns `**word**` markers into bold, higher-contrast text. */
@Composable
private fun emphasised(text: String) = buildAnnotatedString {
    val strong = SpanStyle(
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    text.split("**").forEachIndexed { index, part ->
        if (index % 2 == 1) withStyle(strong) { append(part) } else append(part)
    }
}
