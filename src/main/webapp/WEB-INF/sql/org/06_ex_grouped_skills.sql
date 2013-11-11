--create table
 DROP TABLE IF EXISTS  ex_grouped_skills CASCADE;

create table  ex_grouped_skills as
	select count( ts2__contact__c) as count,   c."ts2__skill_name__c"  as name, c.ts2__rating__c as rating from
	ts2__skill__c  c where   c."ts2__skill_name__c"  !=''  group by   c."ts2__skill_name__c", c.ts2__rating__c;


	CREATE INDEX ex_grouped_skills_idx_name
	  ON ex_grouped_skills
	  USING btree
	  (name COLLATE pg_catalog."default");
