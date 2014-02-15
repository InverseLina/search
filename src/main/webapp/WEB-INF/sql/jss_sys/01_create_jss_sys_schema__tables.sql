-- SCRIPTS
CREATE TABLE if not exists org
(
  id serial NOT NULL,
  name character varying(255) NOT NULL,
  schemaname character varying(64) NOT NULL,
  sfid character varying(255),
  CONSTRAINT org_pkey PRIMARY KEY (id),
  CONSTRAINT unique_name UNIQUE (name)
);
-- SCRIPTS
CREATE TABLE if not exists config
(
  id serial NOT NULL,
  org_id integer,
  name character varying(255) NOT NULL,
  value character varying(512),
  val_text text,
  CONSTRAINT config_pkey PRIMARY KEY (id)
);
-- SCRIPTS
CREATE TABLE if not exists city
(
  id serial NOT NULL,
  name character varying(64) NOT NULL,
  longitude  double precision,
  latitude  double precision,
  CONSTRAINT city_pkey PRIMARY KEY (id)
);

