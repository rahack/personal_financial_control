package ru.pfc.shared

import kotlin.test.Test
import kotlin.test.assertEquals

/** Подтверждает, что тесты общего модуля запускаются на всех целях. */
class SmokeTest {
    @Test
    fun testsRun() {
        assertEquals(4, 2 + 2)
    }
}
