package com.template.contract;

import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TransactionForContract;
import net.corda.core.crypto.SecureHash;

/**
 * Define your contract here.
 */
public class TemplateContract implements Contract {
    /**
     * The verify() function of the contract of each of the transaction's input and output states must not throw an
     * exception for a transaction to be considered valid.
     */
    @Override
    public void verify(TransactionForContract tx) {}

    /** A reference to the underlying legal contract template and associated parameters. */
    private final SecureHash legalContractReference = SecureHash.sha256("Prose contract.");
    @Override public final SecureHash getLegalContractReference() { return legalContractReference; }
}