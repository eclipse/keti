CREATE TABLE policy_set (
  id bigserial NOT NULL primary key,
  client_id varchar(128) NOT NULL,
  issuer_id integer NOT NULL,
  policy_set_id varchar(128) NOT NULL,
  policy_set_json text NOT NULL
);

CREATE TABLE resource (
  id bigserial NOT NULL primary key,
  issuer_id integer NOT NULL,
  client_id varchar(128) NOT NULL,
  resource_identifier varchar(128) NOT NULL,
  attributes text NOT NULL
);

CREATE TABLE subject (
  id bigserial NOT NULL primary key,
  issuer_id integer NOT NULL,
  client_id varchar(128) NOT NULL,
  subject_identifier varchar(128) NOT NULL,
  attributes text NOT NULL
);

CREATE TABLE issuer (
  id bigserial NOT NULL primary key,
  issuer_id varchar(128) NOT NULL,
  issuer_check_token_url varchar(128) NOT NULL
);

ALTER TABLE policy_set ADD CONSTRAINT unique_issuer_client_pset UNIQUE (issuer_id, client_id, policy_set_id);

ALTER TABLE issuer ADD CONSTRAINT unique_issuer_id UNIQUE (issuer_id);

ALTER TABLE resource ADD CONSTRAINT unique_issuer_client_resource_identifier UNIQUE (issuer_id, client_id, resource_identifier);

ALTER TABLE subject ADD CONSTRAINT unique_issuer_client_subject_identifier UNIQUE (issuer_id, client_id, subject_identifier);