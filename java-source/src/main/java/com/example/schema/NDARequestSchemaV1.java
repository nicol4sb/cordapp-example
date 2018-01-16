package com.example.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

/**
 * An IOUState schema.
 */
public class NDARequestSchemaV1 extends MappedSchema {
    public NDARequestSchemaV1() {
        super(NDARequestSchema.class, 1, ImmutableList.of(PersistentNDARequest.class));
    }

    @Entity
    @Table(name = "ndarequest_states")
    public static class PersistentNDARequest extends PersistentState {
        @Column(name = "ndaRequestEmitter") private final String ndaRequestEmitter;
        @Column(name = "ndaRequestRecipient") private final String ndaRequestRecipient;
        @Column(name = "ndaRequestText") private final String ndaRequestText;
        @Column(name = "linear_id") private final UUID linearId;

        public PersistentNDARequest(String ndaRequestEmitter, String ndaRequestRecipient, String ndaRequestText,
				UUID linearId) {
			super();
			this.ndaRequestEmitter = ndaRequestEmitter;
			this.ndaRequestRecipient = ndaRequestRecipient;
			this.ndaRequestText = ndaRequestText;
			this.linearId = linearId;
		}

		public String getNdaRequestEmitter() {
			return ndaRequestEmitter;
		}

		public String getNdaRequestRecipient() {
			return ndaRequestRecipient;
		}

		public String getNdaRequestText() {
			return ndaRequestText;
		}

		public UUID getLinearId() {
			return linearId;
		}
        
    }
}