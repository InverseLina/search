DROP TABLE if EXISTS savedsearches;

CREATE TABLE savedsearches
(
  id bigserial NOT NULL,
  user_id bigint,
  name character varying(64) NOT NULL,
  create_date timestamp ,
  update_date timestamp,
  search character varying(255) NOT NULL,
  CONSTRAINT pk_savedsearched PRIMARY KEY (id),
  CONSTRAINT unq_name UNIQUE (name)
);

DROP TABLE if EXISTS "user";
CREATE TABLE "user"
(
  id bigserial NOT NULL,
  sfid character varying(255) NOT NULL,
  ctoken character varying(255) NOT NULL,
  CONSTRAINT pk_user PRIMARY KEY (id)
);