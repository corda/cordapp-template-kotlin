package com.template

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import net.corda.finance.contracts.asset.Cash
import java.math.BigDecimal

// *****************
// * Contract Code *
// *****************
// This is used to identify our contract when building a transaction
val FX_CONTRACT_ID = "com.template.FxContract"

open class FxContract : Contract {
    // A transaction is considered valid if the verify() function of the contract of each of the transaction's input
    // and output states does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // We should only ever receive one command at a time, else throw an exception. For now, we only have one command 'exchange'
        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value) {
            is Commands.Exchange -> {
                requireThat {
                    val cashInputs = tx.inputsOfType<Cash.State>()
                    val cashOutputs = tx.outputsOfType<Cash.State>()
                    val cashInputsCurrencies = cashInputs.map { it.amount.token.product.currencyCode }.distinct()
                    val cashOutputsCurrencies = cashOutputs.map { it.amount.token.product.currencyCode }.distinct()

                    "Cash inputs from both participants are consumed" using (cashInputs.map { it.owner }.distinct().size == 2)
                    // For now we'll just use currency codes. A more robust way might be to use issuer to determine currency uniqueness
                    "Cash inputs must be of two different currencies" using (cashInputsCurrencies.size == 2)

                    "Cash outputs for both participants are created" using (cashOutputs.map { it.owner }.size == 2)
                    "Cash outputs must be of two different currencies" using (cashOutputsCurrencies.size == 2)

                    "No other states are consumed" using (tx.inputs.size == cashInputs.size)
                    "FX state is the only other state created" using (tx.outputs.size == cashOutputs.size + 1)
                    "FX transactions must be timestamped" using (tx.timeWindow != null)

                    // Our Fx State is both created and consumed in one transaction
                    val fxOutput = tx.outputsOfType<FxState>().single()
                    val fxCurrencies = listOf(fxOutput.currencyOne.token.currencyCode, fxOutput.currencyTwo.token.currencyCode)

                    "Cash inputs currencies match those specified in the fx state" using (cashInputsCurrencies.containsAll(fxCurrencies))
                    "Cash outputs currencies match those specified in the fx state" using (cashOutputsCurrencies.containsAll(fxCurrencies))

                    val spotRate = tx.commands.requireSingleCommand<FxContract.OracleCommand>().value.spotRate
                    "Spot rate is greater than zero" using (tx.timeWindow != null)

                    "Ratio of cash is equal to spot rate" using (BigDecimal(fxOutput.currencyTwo.quantity / fxOutput.currencyOne.quantity) == spotRate)
                }
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Exchange : Commands
    }

    class OracleCommand(val spotRate: BigDecimal) : CommandData
}