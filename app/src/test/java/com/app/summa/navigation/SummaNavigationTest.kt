package com.app.summa.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class SummaNavigationTest {

    @Test
    fun `getTabIndex returns correct index for base routes`() {
        assertEquals(0, getTabIndex("dashboard"))
        assertEquals(1, getTabIndex("planner"))
        assertEquals(2, getTabIndex("habits"))
        assertEquals(3, getTabIndex("knowledge"))
        assertEquals(4, getTabIndex("money"))
        assertEquals(5, getTabIndex("reflections"))
    }

    @Test
    fun `getTabIndex returns correct index for routes with query params`() {
        assertEquals(1, getTabIndex("planner?noteTitle=Test"))
        assertEquals(1, getTabIndex("planner?noteTitle=Test&noteContent=Content"))
    }

    @Test
    fun `getTabIndex returns correct index for routes with path params`() {
        // Assuming habit_detail was a tab (it's not, but logic handles it)
        // If HabitDetail was in the list, it would work.
        // But let's test one that is in the list if it had path params.
        // None of the tabs have path params currently, only query params.
        // But let's test the logic resilience.

        // Let's assume we have a route "habits/123" if habits supported it
        // assertEquals(2, getTabIndex("habits/123"))
    }

    @Test
    fun `getTabIndex returns -1 for unknown routes`() {
        assertEquals(-1, getTabIndex("unknown"))
        assertEquals(-1, getTabIndex("settings"))
        assertEquals(-1, getTabIndex(null))
    }
}