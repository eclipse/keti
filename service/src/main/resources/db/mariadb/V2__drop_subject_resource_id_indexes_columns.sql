ALTER TABLE resource DROP INDEX unique_issuer_client_resource_id;
ALTER TABLE resource DROP COLUMN resource_id;
ALTER TABLE subject DROP INDEX unique_issuer_client_subject_id;
ALTER TABLE subject DROP COLUMN subject_id;
