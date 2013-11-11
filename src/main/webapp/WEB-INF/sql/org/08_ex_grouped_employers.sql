-- SCRIPTS
 DROP TABLE IF EXISTS  ex_grouped_employers CASCADE;

-- SCRIPTS
 create table  ex_grouped_employers as
 SELECT count(c.ts2__contact__c) AS count, c.ts2__name__c AS name,
    date_part('year', age(c.ts2__employment_end_date__c, c.ts2__employment_start_date__c)) AS age
   FROM ts2__employment_history__c c
  WHERE c.ts2__name__c <> ''
  GROUP BY c.ts2__name__c, date_part('year', age(c.ts2__employment_end_date__c, c.ts2__employment_start_date__c));

-- SCRIPTS
	CREATE INDEX ex_grouped_employers_idx_name
	  ON ex_grouped_employers
	  USING btree
	  (name COLLATE pg_catalog."default");



