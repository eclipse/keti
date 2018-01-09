ALTER TABLE resource ADD CONSTRAINT unique_zid_resource_identifier UNIQUE (authorization_zone_id, resource_identifier);
ALTER TABLE subject ADD CONSTRAINT unique_zid_subject_identifier UNIQUE (authorization_zone_id, subject_identifier);
ALTER TABLE policy_set ADD CONSTRAINT unique_zid_pset UNIQUE (authorization_zone_id, policy_set_id);

ALTER TABLE resource ADD FOREIGN KEY (authorization_zone_id) REFERENCES authorization_zone(id);
ALTER TABLE subject ADD FOREIGN KEY (authorization_zone_id) REFERENCES authorization_zone(id);
ALTER TABLE policy_set ADD FOREIGN KEY (authorization_zone_id) REFERENCES authorization_zone(id);
ALTER TABLE authorization_zone_client ADD FOREIGN KEY (authorization_zone_id) REFERENCES authorization_zone(id);