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
  org_id int not null,
  name character varying(45) NOT NULL,
  value character varying(45),
  CONSTRAINT config_pkey PRIMARY KEY (org_id, name)
)



