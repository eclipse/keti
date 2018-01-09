
ALTER TABLE resource DROP CONSTRAINT unique_issuer_client_resource_identifier;
ALTER TABLE resource DROP COLUMN issuer_id;
ALTER TABLE resource DROP COLUMN client_id;

ALTER TABLE subject DROP CONSTRAINT unique_issuer_client_subject_identifier;
ALTER TABLE subject DROP COLUMN issuer_id;
ALTER TABLE subject DROP COLUMN client_id;

ALTER TABLE policy_set DROP CONSTRAINT unique_issuer_client_pset;
ALTER TABLE policy_set DROP COLUMN issuer_id;
ALTER TABLE policy_set DROP COLUMN client_id;

ALTER TABLE resource ADD CONSTRAINT unique_zid_resource_identifier UNIQUE (authorization_zone_id, resource_identifier);
ALTER TABLE subject ADD CONSTRAINT unique_zid_subject_identifier UNIQUE (authorization_zone_id, subject_identifier);
ALTER TABLE policy_set ADD CONSTRAINT unique_zid_pset UNIQUE (authorization_zone_id, policy_set_id);

ALTER TABLE resource ADD FOREIGN KEY (authorization_zone_id) REFERENCES authorization_zone(id) ON DELETE CASCADE;
ALTER TABLE subject ADD FOREIGN KEY (authorization_zone_id) REFERENCES authorization_zone(id) ON DELETE CASCADE;
ALTER TABLE policy_set ADD FOREIGN KEY (authorization_zone_id) REFERENCES authorization_zone(id) ON DELETE CASCADE;
ALTER TABLE authorization_zone_client ADD FOREIGN KEY (authorization_zone_id) REFERENCES authorization_zone(id) ON DELETE CASCADE;