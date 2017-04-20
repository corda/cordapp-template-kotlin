package com.template.state;

import com.template.contract.TemplateContract;
import net.corda.core.contracts.ContractState;

import java.security.PublicKey;
import java.util.Collections;
import java.util.List;

/**
 * Define your state object here.
 */
public class TemplateState implements ContractState {
    private final TemplateContract contract;

    public TemplateState(TemplateContract contract) { this.contract = contract; }

    @Override public TemplateContract getContract() { return contract; }

    /** The public keys of the involved parties. */
    @Override public List<PublicKey> getParticipants() { return Collections.emptyList(); }
}