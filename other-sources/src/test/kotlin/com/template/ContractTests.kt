package com.template

import net.corda.testing.setCordappPackages
import net.corda.testing.unsetCordappPackages
import org.junit.After
import org.junit.Before
import org.junit.Test

class ContractTests {

    @Before
    fun setup() {
        setCordappPackages("com.template")
    }

    @After
    fun tearDown() {
        unsetCordappPackages()
    }

    @Test
    fun `dummy test`() = Unit
}