-- SCRIPTS
CREATE TABLE if not exists zipcode_us
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
  -- SCRIPTS
  DO $$
  BEGIN
  IF NOT EXISTS (
      select 1 from pg_extension e where e."extname" = 'pg_trgm'
      ) THEN
    CREATE EXTENSION pg_trgm   SCHEMA pg_catalog;
  END IF;
  END$$;


-- SCRIPTS
  DO $$
  BEGIN
  IF NOT EXISTS (
      SELECT 1
      FROM   pg_class c
      JOIN   pg_namespace n ON n.oid = c.relnamespace
      WHERE  c.relname = 'zipcode_idx_city_gin'
      AND    n.nspname =   'jss_sys'
      ) THEN
    CREATE INDEX zipcode_idx_city_gin
    ON jss_sys.zipcode_us
    USING gin
    (city COLLATE pg_catalog."default" gin_trgm_ops);
  END IF;
  END$$;