#!//usr/bin/env jython.sh
# Example python (via jython) code to use the RPC interface, return transactions and then delve deeper into those types
# Works best against a node that has generated some cash issuance txns.

from net.corda.client import CordaRPCClient
from net.corda.node.services.config.ConfigUtilitiesKt import configureTestSSL
from com.google.common.net import HostAndPort
from net.corda.contracts.asset import Cash

import java.util.Currency

# Normally there would be an if main conditional + indent here, but this is for easier experimenting, cut pasting etc.

client = CordaRPCClient(HostAndPort.fromString("localhost:20004"), configureTestSSL())
client.start("user1", "test")
proxy = client.proxy(None,0)
txns = proxy.verifiedTransactions().first

print "There are %s transactions on the node" % (len(txns))
if len(txns):
    txn1 = txns[0]
    print "First transaction\n-----------------"
    print txn1.tx

    issuing_txns = [x.tx for x in txns if type(x.tx.commands[0].value) == Cash.Commands.Issue ]

    all_usd_issuing_txns = [x for x in issuing_txns if x.outputs[0].data.amount.token.product == java.util.Currency.getInstance("USD")]

    sum_of_all_usd = sum([x.outputs[0].data.amount.quantity for x in all_usd_issuing_txns]) / 100.0 # Amounts are stored in cents, not dollars.

    print "All USD issuing txn amounts\n----------------------------"
    for i in all_usd_issuing_txns:
        print i.outputs[0].data.amount

    print "Total\n-----"
    print sum_of_all_usd







