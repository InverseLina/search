DROP SCHEMA IF EXISTS  jss_sys  CASCADE;
CREATE SCHEMA jss_sys AUTHORIZATION postgres;

SET search_path TO jss_sys;

-- Table: jss_sys.org

-- DROP TABLE jss_sys.org;

CREATE TABLE org
(
  name character varying(255) NOT NULL,
  schemaname character varying(64) NOT NULL,
  sfid character varying(255),
  CONSTRAINT org_pkey PRIMARY KEY (name)
);



