CREATE TABLE zone_issuer (
  issuer_id integer  NOT NULL,
  zone_id integer NOT NULL,
  PRIMARY KEY(issuer_id, zone_id),
  FOREIGN KEY (zone_id) REFERENCES authorization_zone(id),
  FOREIGN KEY (issuer_id) REFERENCES issuer(id)
);

ALTER TABLE authorization_zone_client ADD FOREIGN KEY (issuer_id) REFERENCES issuer(id);
ALTER TABLE issuer ADD CONSTRAINT unique_issuer_check_token_url UNIQUE (issuer_check_token_url);

ALTER TABLE issuer ALTER column issuer_check_token_url TYPE varchar(1024);