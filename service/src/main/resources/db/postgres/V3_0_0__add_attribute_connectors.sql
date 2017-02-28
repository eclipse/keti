CREATE TABLE attribute_connector (
  id bigserial NOT NULL primary key,
  cached_interval_minutes integer DEFAULT 0
);

CREATE TABLE attribute_adapter_connection (
  id bigserial NOT NULL primary key,
  connector_id integer NOT NULL,
  adapter_endpoint varchar(256) NOT NULL,
  adapter_token_url varchar(256) NOT NULL,
  adapter_client_id varchar(128) NOT NULL
);

ALTER TABLE authorization_zone ADD COLUMN resource_attribute_connector bigint NULL;
ALTER TABLE authorization_zone ADD FOREIGN KEY (resource_attribute_connector) REFERENCES attribute_connector(id) ON DELETE CASCADE;
ALTER TABLE authorization_zone ADD COLUMN subject_attribute_connector bigint NULL;
ALTER TABLE authorization_zone ADD FOREIGN KEY (subject_attribute_connector) REFERENCES attribute_connector(id) ON DELETE CASCADE;