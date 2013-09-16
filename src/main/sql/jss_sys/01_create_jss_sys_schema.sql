CREATE TABLE org
(
  id serial NOT NULL,
  name character varying(255) NOT NULL,
  schemaname character varying(64) NOT NULL,
  sfid character varying(255),
  CONSTRAINT org_pkey PRIMARY KEY (id),
  CONSTRAINT unique_name UNIQUE (name)
);

CREATE TABLE config
(
  id serial not null,
  org_id int,
  name character varying(255) NOT NULL,
  value character varying(512),
  CONSTRAINT config_pkey PRIMARY KEY (id)
)



