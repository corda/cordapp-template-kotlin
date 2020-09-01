package com.template.contracts


import net.corda.core.identity.Party

// A turn is a Party and a Bid.
typealias Turn = Pair<Party, Game.Bid>