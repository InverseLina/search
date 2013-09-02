DROP TABLE if EXISTS jss_sys.zipcode_us;

CREATE TABLE jss_sys.zipcode_us
(
  zip character(5) NOT NULL,
  city character varying(64),
  state character(2),
  latitude double precision,
  longitude double precision,
  timezone integer,
  dst integer,
  CONSTRAINT zcta_pkey PRIMARY KEY (zip)
);