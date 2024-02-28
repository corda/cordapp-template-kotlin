package com.r3.developers.apples.contracts

import net.corda.v5.ledger.utxo.Command

interface AppleCommands : Command {
    class Issue : AppleCommands
    class Redeem : AppleCommands
    class PackBasket : AppleCommands
}