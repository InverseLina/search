-- SCRIPTS
  CREATE TABLE if not exists jss_grouped_skills
(
  id serial NOT NULL,
  count bigint,
  name character varying(150),
  CONSTRAINT jss_grouped_skills_pkey PRIMARY KEY (id)
);

-- SCRIPTS

	insert into jss_grouped_skills (count,name)
	select count( ts2__contact__c) as count,   c."ts2__skill_name__c"  as name from 
	ts2__skill__c  c where   c."ts2__skill_name__c"  !=''  group by   c."ts2__skill_name__c"


