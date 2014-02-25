-- SCRIPTS
  CREATE TABLE if not exists jss_grouped_educations
(
  id serial NOT NULL,
  count bigint,
  name character varying(150),
  CONSTRAINT jss_grouped_educations_pkey PRIMARY KEY (id)
);

-- SCRIPTS

  insert into jss_grouped_educations (count,name) 
  select count( ts2__contact__c) as count,   c."ts2__name__c"  as name from 
  ts2__education_history__c  c where   c."ts2__name__c"  !=''  group by   c."ts2__name__c"


