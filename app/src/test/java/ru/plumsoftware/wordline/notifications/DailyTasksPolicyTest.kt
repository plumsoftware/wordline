package ru.plumsoftware.wordline.notifications

import org.junit.Assert.*
import org.junit.Test

class DailyTasksPolicyTest {
    @Test fun notifiesOnlyForNewUnseenDay() {
        assertTrue(shouldNotifyNewTasks("2026-11-22", "2026-11-23", null))
        assertTrue(shouldNotifyNewTasks("2026-11-20", "2026-11-23", "2026-11-22"))
        assertFalse(shouldNotifyNewTasks("2026-11-23", "2026-11-23", null))
        assertFalse(shouldNotifyNewTasks("2026-11-22", "2026-11-23", "2026-11-23"))
        assertFalse(shouldNotifyNewTasks("", "2026-11-23", null))
    }
    @Test fun clockRollbackDoesNotRepeatReminders() {
        assertFalse(shouldNotifyNewTasks("2026-11-24", "2026-11-23", null))
        assertFalse(shouldNotifyNewTasks("2026-11-20", "2026-11-23", "2026-11-24"))
    }
}
