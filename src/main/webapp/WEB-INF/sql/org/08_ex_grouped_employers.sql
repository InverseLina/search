-- SCRIPTS
  CREATE TABLE if not exists jss_grouped_employers
(
  id serial NOT NULL,
  count bigint,
  name character varying(150),
  CONSTRAINT jss_grouped_employers_pkey PRIMARY KEY (id)
);

-- SCRIPTS

  insert into jss_grouped_employers (count,name) 
  select count( ts2__contact__c) as count,   c."ts2__name__c"  as name from 
  ts2__employment_history__c  c where   c."ts2__name__c"  !=''  group by   c."ts2__name__c"


