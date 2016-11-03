from com.r3corda.client import CordaRPCClient
from com.r3corda.node.services.config.ConfigUtilitiesKt import configureTestSSL
from com.google.common.net import HostAndPort

# Normally there would be an if main conditional + indent here, but this is for easier experimenting, cut pasting etc.

client = CordaRPCClient(HostAndPort.fromString("localhost:31337"), configureTestSSL())
client.start("user1", "test")
proxy = client.proxy(None,0)
print proxy.verifiedTransactions()