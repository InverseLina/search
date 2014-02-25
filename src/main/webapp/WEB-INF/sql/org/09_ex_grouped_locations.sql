-- SCRIPTS
DROP TABLE IF EXISTS  jss_grouped_locations CASCADE;

-- SCRIPTS
CREATE TABLE if not exists jss_grouped_locations
(
  id serial NOT NULL,
  count bigint,
  name character varying(150),
  CONSTRAINT jss_grouped_locations_pkey PRIMARY KEY (id)
);

-- SCRIPTS

insert into jss_grouped_locations (count,name)
  select count(c.id), z.city from contact c inner join jss_sys.zipcode_us z on c.mailingpostalcode = z.zip
  group by z.city



