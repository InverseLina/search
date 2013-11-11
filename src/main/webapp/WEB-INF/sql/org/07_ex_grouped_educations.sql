-- SCRIPTS
 DROP TABLE IF EXISTS  ex_grouped_educations CASCADE;
-- SCRIPTS
create table  ex_grouped_educations as
 select count( ts2__contact__c) as count,   c."ts2__name__c"  as name,
date_part('year', age(c.ts2__graduationdate__c, '1970-01-01')) AS age
 from
  ts2__education_history__c  c where   c."ts2__name__c"  !=''  group by   c."ts2__name__c",
 date_part('year', age(c.ts2__graduationdate__c, '1970-01-01'));
  -- SCRIPTS
  CREATE INDEX ex_grouped_educations_idx_name
    ON ex_grouped_educations
    USING btree
    (name COLLATE pg_catalog."default");




