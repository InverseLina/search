DROP TABLE if EXISTS savedsearches;

-- DROP TABLE savedsearches;

CREATE TABLE savedsearches
(
  userid bigint,
  create_date timestamp ,
  update_date timestamp,
  search character varying(255) NOT NULL,
  name character varying(64) NOT NULL,
  id bigserial NOT NULL,
  CONSTRAINT pk_savedsearched PRIMARY KEY (id),
  CONSTRAINT unq_name UNIQUE (name)
);