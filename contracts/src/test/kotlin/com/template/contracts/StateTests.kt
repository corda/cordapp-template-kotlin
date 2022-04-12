package com.template.contracts

import com.template.states.IOUState
import net.corda.core.contracts.Amount
import net.corda.core.identity.Party
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class StateTests {
    @Test
    fun hasFieldOfCorrectType() {
        // Does the field exist?
        IOUState::class.java.getDeclaredField("msg")
        IOUState::class.java.getDeclaredField("amount")
        IOUState::class.java.getDeclaredField("paid")
        IOUState::class.java.getDeclaredField("lender")
        IOUState::class.java.getDeclaredField("borrower")

        // Is the field of the correct type?
        assertEquals(IOUState::class.java.getDeclaredField("msg").type, String()::class.java)
        assertEquals(IOUState::class.java.getDeclaredField("amount").type, Amount::class.java)
        assertEquals(IOUState::class.java.getDeclaredField("paid").type, Amount::class.java)
        assertEquals(IOUState::class.java.getDeclaredField("lender").type, Party::class.java)
        assertEquals(IOUState::class.java.getDeclaredField("borrower").type, Party::class.java)
    }
}