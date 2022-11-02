<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# CorDapp Template - Mahmoud - Banking Network

This Cordapp demonstrates a Banking Network using the Accounts and Tokens SDK.

# Deploying the Nodes
./gradlew clean deployNodes   

# Running the Nodes
./build/nodes/runnodes   

# 1. Issuing Money from the Central Branch to a Local Bank
flow start IssueCurrencyToBranch currency: USD, amount: 100000000, branch: BranchA

# 2. Creating an Account - Branch A
flow start AddAccount name: Mahmoud

# 3. Issuing Money to an Account from the Branch
flow start IssueMoneyToAccount acctName: Mahmoud, amount: 10000, currency: USD

# 4. Creating an Account - Branch B
flow start AddAccount name: John

# 5. Sharing an Account with a Branch - Branch B
flow start ShareAccount acctNameToShare: John, party: BranchA

# 6. Sending money from Account Mahmoud to Account John
flow start SendMoney sender: Mahmoud, receiver: John, amount: 100, currency: USD

# 7. Check Accounts in Vault
run vaultQuery contractStateType: com.r3.corda.lib.accounts.contracts.states.AccountInfo
