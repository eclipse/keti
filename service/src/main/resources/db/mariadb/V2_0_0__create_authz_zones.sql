CREATE TABLE `authorization_zone` (
  `id` int(18) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` varchar(1024) NOT NULL,
  `subdomain` varchar(255) NOT NULL,
  PRIMARY KEY(`id`),
  UNIQUE KEY `name` (`name`),
  UNIQUE KEY `subdomain` (`subdomain`)
) ENGINE=InnoDB DEFAULT CHARSET=UTF8;

CREATE TABLE `authorization_zone_client` (
  `id` int(18) NOT NULL AUTO_INCREMENT,
  `issuer_id` int(18) NOT NULL,
  `client_id` varchar(255) NOT NULL,
  `authorization_zone_id` int(18) NOT NULL,
  PRIMARY KEY(`id`),
  UNIQUE KEY `client_in_zone` (`issuer_id`,`client_id`,`authorization_zone_id`)
) ENGINE=InnoDB DEFAULT CHARSET=UTF8;

ALTER TABLE subject ADD COLUMN authorization_zone_id int(18) DEFAULT 0;
ALTER TABLE resource ADD COLUMN authorization_zone_id int(18) DEFAULT 0;
ALTER TABLE policy_set ADD COLUMN authorization_zone_id int(18) DEFAULT 0;