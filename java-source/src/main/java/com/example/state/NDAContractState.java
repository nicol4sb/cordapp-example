package com.example.state;

import java.util.Arrays;
import java.util.List;

import com.example.schema.NDAContractSchemaV1;
import com.google.common.collect.ImmutableList;

import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;

/**
 * The state object recording NDARequest agreements between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 */
public class NDAContractState implements LinearState, QueryableState {
	private final String ndaRequestText;
	private final Party ndaRequestEmitter;
	private final Party ndaRequestRecipient;
	private final UniqueIdentifier linearId;

	public NDAContractState(String ndaRequestText, Party ndaRequestEmitter, Party ndaRequestRecipient) {
		this.ndaRequestText = ndaRequestText;
		this.ndaRequestEmitter = ndaRequestEmitter;
		this.ndaRequestRecipient = ndaRequestRecipient;
		this.linearId = new UniqueIdentifier();
	}

	public String getNdaRequestText() {
		return ndaRequestText;
	}

	public Party getNdaRequestEmitter() {
		return ndaRequestEmitter;
	}

	public Party getNdaRequestRecipient() {
		return ndaRequestRecipient;
	}

	@Override
	public UniqueIdentifier getLinearId() {
		return linearId;
	}

	@Override
	public List<AbstractParty> getParticipants() {
		return Arrays.asList(ndaRequestEmitter, ndaRequestRecipient);
	}

	@Override
	public PersistentState generateMappedObject(MappedSchema schema) {
		if (schema instanceof NDAContractSchemaV1) {
			return new NDAContractSchemaV1.PersistentNDARequest(this.ndaRequestEmitter.getName().toString(), this.ndaRequestRecipient.getName().toString(),
					this.ndaRequestText, this.linearId.getId());
		} else {
			throw new IllegalArgumentException("Unrecognised schema $schema");
		}
	}

	@Override
	public Iterable<MappedSchema> supportedSchemas() {
		return ImmutableList.of(new NDAContractSchemaV1());
	}

	@Override
	public String toString() {
		return String.format("%s(ndaRequest=%s, emitter=%s, recipient=%s, linearId=%s)", getClass().getSimpleName(), ndaRequestText,
				ndaRequestEmitter, ndaRequestRecipient, linearId);
	}
}