CREATE TABLE authorization_zone (
  id bigserial NOT NULL primary key,
  name varchar(255) NOT NULL,
  description varchar(1024) NOT NULL,
  subdomain varchar(255) NOT NULL
);

CREATE TABLE authorization_zone_client (
  id bigserial NOT NULL primary key,
  issuer_id integer NOT NULL,
  client_id varchar(255) NOT NULL,
  authorization_zone_id integer NOT NULL
);

ALTER TABLE authorization_zone ADD CONSTRAINT name UNIQUE (name);
ALTER TABLE authorization_zone ADD CONSTRAINT subdomain UNIQUE (subdomain);
ALTER TABLE authorization_zone_client ADD CONSTRAINT client_in_zone UNIQUE (issuer_id,client_id,authorization_zone_id);

ALTER TABLE subject ADD COLUMN authorization_zone_id integer DEFAULT 0;
ALTER TABLE resource ADD COLUMN authorization_zone_id integer DEFAULT 0;
ALTER TABLE policy_set ADD COLUMN authorization_zone_id integer DEFAULT 0;