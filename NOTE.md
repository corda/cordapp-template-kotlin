### CorDapp training and exercise by HJ
NOTE: This project is not intended to be complete nor recommended for CorDapp trainees. It is
purely for future reference for HJ. Don't judge me by this!

This exercise is based on [Your First CorDapp](https://training.corda.net/getting-started/set-up-your-computer/). 

**Issue command example** \
PartyA issues an IOUState of £5 with PartyB.

	flow start IssueFlow receiver: "O=PartyB, L=New York, C=US", iouAmount: 500

**Transfer command example**

    flow start TransferFlow stateId: "f24ea147-c2f8-4ef3-8289-38568aa2a342", newLender: "O=Bank, L=New York, C=US"

**Settle command example** \
Borrower (PartyB) pays off £2 to Lender (PartyA).

	flow start SettleFlow lender: "O=PartyA, L=London, C=GB", stateId: "f24ea147-c2f8-4ef3-8289-38568aa2a342", toPay: £2

**See the current IOUState in the vault**

    run vaultTrack contractStateType: com.template.states.IOUState