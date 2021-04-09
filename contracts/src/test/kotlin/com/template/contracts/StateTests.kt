package com.template.contracts

import com.template.states.TemplateState
import org.junit.Test
import kotlin.test.assertEquals

class StateTests {
    @Test
    fun hasFieldOfCorrectType() {
        // Does the field exist?
        TemplateState::class.java.getDeclaredField("msg")
        // Is the field of the correct type?
        assertEquals(TemplateState::class.java.getDeclaredField("msg").type, String()::class.java)
    }
}