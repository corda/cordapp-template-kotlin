package com.template.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.transactions.LedgerTransaction

class GameContract : Contract {

    companion object {
        @JvmStatic
        val ID = "com.template.contracts.GameContract"
    }

    interface GameCommands : CommandData {
        class ProposeGame : TypeOnlyCommandData(), GameCommands
        class StartGame : TypeOnlyCommandData(), GameCommands
        class AddRoll : TypeOnlyCommandData(), GameCommands
        class MakeBid : TypeOnlyCommandData(), GameCommands
        class MakeCall : TypeOnlyCommandData(), GameCommands
        class EndRound : TypeOnlyCommandData(), GameCommands
    }

    override fun verify(tx: LedgerTransaction) = Unit

}