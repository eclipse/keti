ALTER TABLE authorization_zone DROP COLUMN resource_attribute_connector;
ALTER TABLE authorization_zone DROP COLUMN subject_attribute_connector;
DROP TABLE attribute_connector;
DROP TABLE attribute_adapter_connection;

ALTER TABLE authorization_zone ADD resource_attribute_connector_json text NULL;
ALTER TABLE authorization_zone ADD subject_attribute_connector_json text NULL;
