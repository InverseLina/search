CREATE TABLE config
(
  name character varying(45) NOT NULL,
  value character varying(45),
  CONSTRAINT config_pkey PRIMARY KEY (name)
)