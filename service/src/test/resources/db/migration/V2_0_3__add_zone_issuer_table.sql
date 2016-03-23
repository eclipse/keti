CREATE TABLE `zone_issuer` (
  `issuer_id` int(18) NOT NULL,
  `zone_id` int(18) NOT NULL,
  PRIMARY KEY(`issuer_id`, `zone_id`),
  FOREIGN KEY (zone_id) REFERENCES authorization_zone(id) ON DELETE CASCADE,
  FOREIGN KEY (issuer_id) REFERENCES issuer(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=UTF8;


ALTER TABLE authorization_zone_client ADD FOREIGN KEY (issuer_id) REFERENCES issuer(id) ON DELETE CASCADE;
ALTER TABLE issuer ADD CONSTRAINT unique_issuer_check_token_url UNIQUE (issuer_check_token_url);

ALTER TABLE issuer ALTER column issuer_check_token_url varchar(1024) NOT NULL;
