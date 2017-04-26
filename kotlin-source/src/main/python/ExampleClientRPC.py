# Example python (via jython) code to use the client RPC interface
# Works in conjunction with the Example CorDapp nodes.
# The nodes must have transacted for you to see the purchase order states.

import sys
from com.google.common.net import HostAndPort
from net.corda.client.rpc import CordaRPCClient

if len(sys.argv) != 2:
    print("USAGE: ./jython.sh ExampleClientRPC.py [HOST:ARTEMIS_PORT]")
    exit()

client = CordaRPCClient(HostAndPort.fromString(sys.argv[1]), None, None)
client.start("user1", "test")
proxy = client.proxy()
txs = proxy.verifiedTransactions().first

print "There are %s 'unspent' purchase orders on 'NodeA'" % (len(txs))

if len(txs):
    for txn in txs:
        print(txn.tx.outputs[0].data)
