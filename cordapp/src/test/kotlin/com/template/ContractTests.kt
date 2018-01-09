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
    fun `fx tests`() {
        val buyer = MINI_CORP
        val seller = MEGA_CORP
        val fx = FxState(5.POUNDS, 10.DOLLARS, buyer, seller, BigDecimal(2.0))

        ledger {
            transaction("Mini Corp buys USD from Mega Corp using GBP") {
                input(CASH_PROGRAM_ID, 5.POUNDS.CASH `owned by` MINI_CORP)
                input(CASH_PROGRAM_ID, 10.DOLLARS.CASH `owned by` MEGA_CORP)

                output(CASH_PROGRAM_ID, "Mini Corp's £5", 5.POUNDS.CASH `owned by` MEGA_CORP)
                output(CASH_PROGRAM_ID, "Mega Corp's $10", 10.DOLLARS.CASH `owned by` MINI_CORP)
                output(FX_CONTRACT_ID, fx)

                command(MEGA_CORP_PUBKEY, MINI_CORP_PUBKEY) { FxContract.Commands.Exchange() }
                command(MINI_CORP_PUBKEY, MEGA_CORP_PUBKEY) { Cash.Commands.Move() }
                command(ORACLE_PUBKEY) { FxContract.OracleCommand(BigDecimal(2.0)) }

                timeWindow(Instant.now(), Duration.ofSeconds(60))
                verifies()
            }
        }
    }
}