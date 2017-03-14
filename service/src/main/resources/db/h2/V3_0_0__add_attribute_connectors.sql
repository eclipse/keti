CREATE TABLE `attribute_connector` (
  `id` int(18) NOT NULL AUTO_INCREMENT,
  `active` boolean DEFAULT false,
  `cached_interval_minutes` int DEFAULT 0,
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=UTF8;

CREATE TABLE `attribute_adapter_connection` (
  `id` int(18) NOT NULL AUTO_INCREMENT,
  `connector_id` int(18) NOT NULL,
  `adapter_endpoint` varchar(128) NOT NULL,
  `adapter_token_url` varchar(128) NOT NULL,
  `adapter_client_id` varchar(128) NOT NULL,
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=UTF8;

ALTER TABLE authorization_zone ADD resource_attribute_connector int(18) NULL;
ALTER TABLE authorization_zone ADD FOREIGN KEY (resource_attribute_connector) REFERENCES attribute_connector(id) ON DELETE CASCADE;
ALTER TABLE authorization_zone ADD subject_attribute_connector int(18) NULL;
ALTER TABLE authorization_zone ADD FOREIGN KEY (subject_attribute_connector) REFERENCES attribute_connector(id) ON DELETE CASCADE;