-- SCRIPTS
  CREATE TABLE if not exists ex_grouped_skills
(
  id serial NOT NULL,
  count bigint,
  name character varying(150),
  CONSTRAINT ex_grouped_skills_pkey PRIMARY KEY (id)
)

-- SCRIPTS
DO $$
  BEGIN
  IF NOT EXISTS (
      SELECT 1
      FROM   pg_class c
      JOIN   pg_namespace n ON n.oid = c.relnamespace
      WHERE  c.relname = 'ex_grouped_skills_idx_name'
      AND    n.nspname =   current_schema
      ) THEN
     
	CREATE INDEX ex_grouped_skills_idx_name
	  ON ex_grouped_skills
	  USING btree
	  (name COLLATE pg_catalog."default");
  END IF;
  END$$;  

-- SCRIPTS

	insert into ex_grouped_skills (count,name)
	select count( ts2__contact__c) as count,   c."ts2__skill_name__c"  as name from 
	ts2__skill__c  c where   c."ts2__skill_name__c"  !=''  group by   c."ts2__skill_name__c"


