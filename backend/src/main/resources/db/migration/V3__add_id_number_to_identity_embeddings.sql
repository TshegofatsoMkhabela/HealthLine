-- idNumber was missing from V2, leaving no way to recognize a repeat enrollment
-- by the same real person, or to audit which SA ID number an identityId maps to.
-- NOT NULL is safe here — nothing has been deployed to production yet, so there
-- are no existing rows to backfill.
ALTER TABLE identity_embeddings ADD COLUMN id_number VARCHAR(13) NOT NULL;
ALTER TABLE identity_embeddings ADD CONSTRAINT uq_identity_embeddings_id_number UNIQUE (id_number);
