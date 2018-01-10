package com.template

import net.corda.finance.DOLLARS
import net.corda.finance.POUNDS
import net.corda.finance.contracts.asset.*
import net.corda.testing.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import java.time.Instant

class ContractTests {

    @Before
    fun setup() {
        setCordappPackages("com.template", "net.corda.finance.contracts.asset")
    }

    @After
    fun tearDown() {
        unsetCordappPackages()
    }

    @Test
    fun `dummy test`() {

    }
}