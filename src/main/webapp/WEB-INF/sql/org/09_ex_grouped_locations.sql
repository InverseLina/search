-- SCRIPTS
 DROP TABLE IF EXISTS  ex_grouped_locations CASCADE;

-- SCRIPTS
 create table  ex_grouped_locations as
  SELECT
    count(C.id)  AS count , z.city as name, c.mailingpostalcode  AS zip,
    public.earth_distance(public.ll_to_earth(z."latitude", z."longitude"),
                          public.ll_to_earth(c."ts2__latitude__c", c."ts2__longitude__c")) / 1000 AS distance
  FROM (SELECT
          *
        FROM jss_sys.zipcode_us z) z INNER JOIN PUBLIC.contact C
      ON c.mailingpostalcode = z.zip
  GROUP BY z.city, c."mailingpostalcode",
    public.earth_distance(public.ll_to_earth(z."latitude", z."longitude"),
                          public.ll_to_earth(c."ts2__latitude__c", c."ts2__longitude__c")) / 1000;

-- SCRIPTS
	CREATE INDEX ex_grouped_locations_idx_city
	  ON ex_grouped_locations
	  USING btree
	  (name COLLATE pg_catalog."default");
-- SCRIPTS
	CREATE INDEX ex_grouped_locations_idx_zip
	  ON ex_grouped_locations
	  USING btree
	  (zip COLLATE pg_catalog."default");



