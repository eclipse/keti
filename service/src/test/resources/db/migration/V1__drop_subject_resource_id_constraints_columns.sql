ALTER TABLE resource DROP CONSTRAINT unique_issuer_client_resource_id;
ALTER TABLE resource DROP COLUMN resource_id;
ALTER TABLE subject DROP CONSTRAINT unique_issuer_client_subject_id;
ALTER TABLE subject DROP COLUMN subject_id;