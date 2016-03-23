CREATE TABLE `policy_set` (
  `id` int(18) NOT NULL AUTO_INCREMENT,
  `client_id` varchar(128) NOT NULL,
  `issuer_id` varchar(128) NOT NULL,
  `policy_set_id` varchar(128) NOT NULL,
  `policy_set_json` MEDIUMTEXT NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=UTF8;

CREATE TABLE `resource` (
  `id` int(18) NOT NULL AUTO_INCREMENT,
  `issuer_id` int(18) NOT NULL,
  `client_id` varchar(128) NOT NULL,
  `resource_identifier` varchar(128) NOT NULL,
  `resource_id` varchar(128) NOT NULL,
  `attributes` MEDIUMTEXT NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=UTF8;

CREATE TABLE `subject` (
  `id` int(18) NOT NULL AUTO_INCREMENT,
  `issuer_id` int(18) NOT NULL,
  `client_id` varchar(128) NOT NULL,
  `subject_identifier` varchar(128) NOT NULL,
  `subject_id` varchar(128) NOT NULL,
  `attributes` MEDIUMTEXT NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=UTF8;

CREATE TABLE `issuer` (
  `id` int(18) NOT NULL AUTO_INCREMENT,
  `issuer_id` varchar(128) NOT NULL,
  `issuer_check_token_url` varchar(128) NOT NULL,
  PRIMARY KEY (`id`)  
) ENGINE=InnoDB DEFAULT CHARSET=UTF8;

ALTER TABLE policy_set ADD CONSTRAINT unique_issuer_client_pset UNIQUE (issuer_id, client_id, policy_set_id);

ALTER TABLE issuer ADD CONSTRAINT unique_issuer_id UNIQUE (issuer_id);

ALTER TABLE resource ADD CONSTRAINT unique_issuer_client_resource_identifier UNIQUE (issuer_id, client_id, resource_identifier);
ALTER TABLE resource ADD CONSTRAINT unique_issuer_client_resource_id UNIQUE (issuer_id, client_id, resource_id);

ALTER TABLE subject ADD CONSTRAINT unique_issuer_client_subject_id UNIQUE (issuer_id, client_id, subject_id);
ALTER TABLE subject ADD CONSTRAINT unique_issuer_client_subject_identifier UNIQUE (issuer_id, client_id, subject_identifier);

