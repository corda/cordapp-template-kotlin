package com.example.contract;

import com.example.model.PurchaseOrder;
import kotlin.Unit;
import net.corda.core.Utils;
import net.corda.core.contracts.*;
import net.corda.core.contracts.TransactionForContract.InOutGroup;
import net.corda.core.contracts.clauses.*;
import net.corda.core.crypto.SecureHash;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static kotlin.collections.CollectionsKt.single;
import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * A implementation of a basic smart contract in Corda.
 *
 * This contract facilitates the business logic required for two parties to come to an agreement over a newly issued
 * [PurchaseOrderState], which in turn, encapsulates a [PurchaseOrder].
 *
 * For a new [PurchaseOrderState] to be issued onto the ledger a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [PurchaseOrderState].
 * - An Issue() command with the public keys of the buyer and seller parties.
 * - A timestamp.
 *
 * The contract code (implemented within the [Timestamped] and [Issue] clauses) is run when the transaction is
 * verified via the verify() function.
 * - [Timestamped] checks for the existence of a timestamp
 * - [Issue] runs a series of constraints, see more below
 *
 * All contracts must sub-class the [Contract] interface.
 */
public class PurchaseOrderContract implements Contract {
    /**
     * This is a reference to the underlying legal contract template and associated parameters.
     */
    private final SecureHash legalContractReference = SecureHash.sha256("purchase order contract template and params");
    @Override public final SecureHash getLegalContractReference() { return legalContractReference; }

    /**
     * Filters the command list by type, party and public key all at once.
     */
    private List<AuthenticatedObject<Commands>> extractCommands(TransactionForContract tx) {
        return tx.getCommands()
                .stream()
                .filter(command -> command.getValue() instanceof Commands)
                .map(command -> new AuthenticatedObject<>(
                        command.getSigners(),
                        command.getSigningParties(),
                        (Commands) command.getValue()))
                .collect(toList());
    }

    /**
     * The AllComposition() clause mandates that all specified clauses clauses (in this case [Timestamped] and [Group])
     * must be executed and valid for a transaction involving this type of contract to be valid.
     */
    @Override
    public void verify(TransactionForContract tx) {
        ClauseVerifier.verifyClause(
                tx,
                new AllComposition<>(new Clauses.Timestamp(), new Clauses.Group()),
                extractCommands(tx));
    }

    /**
     * Currently this contract only implements one command. If you wish to add further commands to perhaps Amend() or
     * Cancel() a purchase order, you would add them here. You would then need to add associated clauses to handle
     * transaction verification for the new commands.
     */
    public interface Commands extends CommandData {
        class Place implements IssueCommand, Commands {
            private final long nonce = Utils.random63BitValue();
            @Override public long getNonce() { return nonce; }
        }
    }

    /**
     * This is where we implement our clauses.
     */
    public interface Clauses {
        /**
         * Checks for the existence of a timestamp.
         */
        class Timestamp extends Clause<ContractState, Commands, Unit> {
            @Override public Set<Commands> verify(TransactionForContract tx,
                List<? extends ContractState> inputs,
                List<? extends ContractState> outputs,
                List<? extends AuthenticatedObject<? extends Commands>> commands,
                Unit groupingKey) {

                requireNonNull(tx.getTimestamp(), "must be timestamped");

                // We return an empty set because we don't process any commands
                return Collections.emptySet();
            }
        }

        // If you add additional clauses, make sure to reference them within the 'AnyComposition()' clause.
        class Group extends GroupClauseVerifier<PurchaseOrderState, Commands, UniqueIdentifier> {
            public Group() { super(new AnyComposition<>(new Clauses.Place())); }

            @Override public List<InOutGroup<PurchaseOrderState, UniqueIdentifier>> groupStates(TransactionForContract tx) {
                // Group by purchase order linearId for in/out states.
                return tx.groupStates(PurchaseOrderState.class, PurchaseOrderState::getLinearId);
            }
        }

        /**
         * Checks various requirements for the placement of a purchase order.
         */
        class Place extends Clause<PurchaseOrderState, Commands, UniqueIdentifier> {
            @Override public Set<Commands> verify(TransactionForContract tx,
                List<? extends PurchaseOrderState> inputs,
                List<? extends PurchaseOrderState> outputs,
                List<? extends AuthenticatedObject<? extends Commands>> commands,
                UniqueIdentifier groupingKey)
            {
                final AuthenticatedObject<Commands.Place> command = requireSingleCommand(tx.getCommands(), Commands.Place.class);
                final PurchaseOrderState out = single(outputs);
                final Instant time = tx.getTimestamp().getMidpoint();

                requireThat(require -> {
                    // Generic constraints around generation of the issue purchase order transaction.
                    require.by("No inputs should be consumed when issuing a purchase order.",
                            inputs.isEmpty());
                    require.by("Only one output state should be created for each group.",
                            outputs.size() == 1);
                    require.by("The buyer and the seller cannot be the same entity.",
                            out.getBuyer() != out.getSeller());
                    require.by("All of the participants must be signers.",
                            command.getSigners().containsAll(out.getParticipants()));

                    // Purchase order specific constraints.
                    require.by("We only deliver to the UK.",
                            out.getPurchaseOrder().getDeliveryAddress().getCountry().equals("UK"));
                    require.by("You must order at least one type of item.",
                            !out.getPurchaseOrder().getItems().isEmpty());
                    require.by("You cannot order zero or negative amounts of an item.",
                            out.getPurchaseOrder().getItems().stream().allMatch(item -> item.getAmount() > 0));
                    require.by("You can only order up to 100 items in total.",
                            out.getPurchaseOrder().getItems().stream().mapToInt(PurchaseOrder.Item::getAmount).sum() <= 100);
                    require.by("The delivery date must be in the future.",
                            out.getPurchaseOrder().getDeliveryDate().toInstant().isAfter(time));

                    return null;
                });

                return Collections.singleton(command.getValue());
            }
        }
    }
}