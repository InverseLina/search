-- SCRIPTS
	CREATE TABLE if not exists jss_contact
	(
	  id bigint NOT NULL,
	  resume_tsv tsvector,
	  skills_tsv tsvector,
	  contact_tsv tsvector,
	  sfid character varying(18),
	  resume_lower text,
	  CONSTRAINT jss_contact_pkey PRIMARY KEY (id),
	  CONSTRAINT fk_jss_contact_contact FOREIGN KEY (id)
	      REFERENCES contact (id) MATCH SIMPLE
	      ON UPDATE NO ACTION ON DELETE CASCADE
	)
	WITH (
	  OIDS=FALSE
	);
    
-- SCRIPTS
	CREATE OR REPLACE FUNCTION update_context_ex_resume() RETURNS trigger AS $Body$
	declare schema_name varchar; 
	BEGIN
	schema_name := '"'||TG_TABLE_SCHEMA||'"';
	EXECUTE   'set search_path to '||schema_name;
	IF( TG_OP='INSERT' ) THEN
	    insert into jss_contact(id,resume_lower,resume_tsv,contact_tsv,sfid) values(new.id,lower(coalesce(new."ts2__text_resume__c")),to_tsvector(coalesce(new."ts2__text_resume__c",' ')),to_tsvector(coalesce(new."firstname",' ')||coalesce(new."lastname",' ')||coalesce(new."title",' ')),coalesce(new."sfid",' '));
	ELSIF (TG_OP = 'UPDATE') THEN
	    UPDATE jss_contact SET resume_lower=lower(coalesce(new."ts2__text_resume__c")),resume_tsv=to_tsvector(coalesce(new."ts2__text_resume__c",' ')),contact_tsv= to_tsvector(coalesce(new."firstname",' ')||coalesce(new."lastname",' ')||coalesce(new."title",' '))  where id = new.id;
	END IF;
	RETURN NEW;
	END;
	$Body$
	LANGUAGE 'plpgsql';
	
-- SCRIPTS
	DROP TRIGGER if exists contact_trg_resume_tsv ON contact  CASCADE;
-- SCRIPTS
CREATE TRIGGER contact_trg_resume_tsv
  AFTER INSERT OR UPDATE OF "ts2__text_resume__c","firstname","lastname","title","sfid"
  ON contact
  FOR EACH ROW
  EXECUTE PROCEDURE update_context_ex_resume();
  
-- SCRIPTS
CREATE TABLE if not exists jss_savedsearches
(
  id bigserial NOT NULL,
  user_id bigint,
  name character varying(64) NOT NULL,
  create_date timestamp ,
  update_date timestamp,
  search text NOT NULL,
  CONSTRAINT pk_jss_savedsearched PRIMARY KEY (id),
  CONSTRAINT unq_name UNIQUE (name)
);


-- SCRIPTS
CREATE TABLE if not exists "jss_user"
(
  id bigserial NOT NULL,
  sfid character varying(255) NULL,
  ctoken character varying(255) NOT NULL,
  rtoken character varying(255),
  timeout bigint not null,
  CONSTRAINT pk_jss_user PRIMARY KEY (id)
);


-- SCRIPTS
CREATE TABLE if not exists jss_searchlog
(
  id bigserial NOT NULL,
  user_id bigint NOT NULL,
  date timestamp with time zone,
  search character varying(512),
  perfcount bigint NOT NULL DEFAULT 0,
  perffetch bigint NOT NULL DEFAULT 0,
  CONSTRAINT jss_searchlog_pkey PRIMARY KEY (id)
);
 
-- SCRIPTS
CREATE TABLE if not exists jss_pref
(
  id bigserial NOT NULL,
  user_id bigint NOT NULL,
  name character varying(32),
  val character varying(128),
  val_text text,
  CONSTRAINT pref_pkey PRIMARY KEY (id)
);

-- SCRIPTS
CREATE TABLE if not exists jss_contact_jss_groupby_skills
(
  id serial NOT NULL,
  jss_groupby_skills_id bigint NOT NULL,
  jss_contact_id bigint NOT NULL,
  rating double precision
);
-- SCRIPTS
CREATE TABLE if not exists jss_contact_jss_groupby_educations
(
  id serial NOT NULL,
  jss_groupby_educations_id bigint NOT NULL,
  jss_contact_id bigint NOT NULL
);
-- SCRIPTS
CREATE TABLE if not exists jss_contact_jss_groupby_employers
(
  id serial NOT NULL,
  jss_groupby_employers_id bigint NOT NULL,
  jss_contact_id bigint NOT NULL,
  year double precision
);

-- SCRIPTS
DROP FUNCTION if exists update_ex_group_skills() cascade;

-- SCRIPTS
DROP FUNCTION if exists update_jss_contact_groupby_skills() cascade;

-- SCRIPTS
CREATE OR REPLACE FUNCTION update_skill_related_tables() RETURNS trigger AS $BODY$ 
  DECLARE schema_name varchar; 
  DECLARE  c integer; 
  BEGIN
  schema_name := '"'||TG_TABLE_SCHEMA||'"';
  EXECUTE   'set search_path to '||schema_name;
      IF(TG_OP = 'UPDATE' OR TG_OP = 'DELETE') THEN
      
           delete from jss_contact_jss_groupby_skills t
            where t.jss_contact_id = (select id from contact where sfid = OLD.ts2__contact__c) 
            and t.jss_groupby_skills_id = (select id from jss_grouped_skills where name = OLD.ts2__skill_name__c);
            
            c:=(select "count" from jss_grouped_skills where name = OLD.ts2__skill_name__c);
              
            IF (c=1) THEN 
                DELETE FROM  jss_grouped_skills where name = OLD.ts2__skill_name__c;
            ELSE 
                UPDATE jss_grouped_skills set count=(c-1) where name = OLD.ts2__skill_name__c;
            END IF;

      END IF;

      IF(TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN
        
	        SELECT jss_grouped_skills.count into c from jss_grouped_skills where name = NEW.ts2__skill_name__c limit 1;
	        IF FOUND THEN
	           UPDATE jss_grouped_skills set count=(c+1) where name = NEW.ts2__skill_name__c;
	        ELSE
	           INSERT into jss_grouped_skills(count,name) values(1,NEW.ts2__skill_name__c);
	        END IF; 
	        
	  END IF;
      
      insert into jss_contact_jss_groupby_skills (jss_contact_id,jss_groupby_skills_id,rating)
      select  c.id as jss_contact_id, gskill.id as jss_groupby_skills_id, max(skill.ts2__rating__c) from ts2__skill__c skill 
      inner join contact c on skill.ts2__contact__c = c.sfid 
      inner join jss_grouped_skills gskill on gskill.name = skill.ts2__skill_name__c
      where skill.ts2__skill_name__c = NEW.ts2__skill_name__c and skill.ts2__contact__c = NEW.ts2__contact__c 
      group by c.id, gskill.id;

      RETURN NEW;
  		EXCEPTION 
  			WHEN unique_violation THEN
  			raise notice 'error';
        	RETURN NEW;
      END;  
   $BODY$
    LANGUAGE plpgsql;
-- SCRIPTS
  DROP trigger if exists skill_trigger_group on ts2__skill__c;

-- SCRIPTS
  DROP trigger if exists skill_trigger on ts2__skill__c;
  
-- SCRIPTS
  CREATE  TRIGGER skill_trigger
    AFTER INSERT OR UPDATE OF ts2__skill_name__c,ts2__contact__c
    ON ts2__skill__c
    FOR EACH ROW
    EXECUTE PROCEDURE update_skill_related_tables();

-- SCRIPTS
DROP FUNCTION if exists update_ex_group_educations() cascade;

-- SCRIPTS
DROP FUNCTION if exists update_jss_contact_groupby_educations() cascade;

-- SCRIPTS
CREATE OR REPLACE FUNCTION update_educations_related_tables() RETURNS trigger AS $BODY$ 
  DECLARE schema_name varchar; 
  DECLARE  c integer; 
  BEGIN
  schema_name := '"'||TG_TABLE_SCHEMA||'"';
  EXECUTE   'set search_path to '||schema_name;
      IF(TG_OP = 'UPDATE' OR TG_OP = 'DELETE') THEN

            delete from jss_contact_jss_groupby_educations t
            where t.jss_contact_id = (select id from contact where sfid = OLD.ts2__contact__c) 
            and t.jss_groupby_educations_id = (select id from jss_grouped_educations where name = OLD.ts2__name__c);            
           
            c:=(select "count" from jss_grouped_educations where name = OLD.ts2__name__c);
              
            IF (c=1) THEN 
                DELETE FROM  jss_grouped_educations where name = OLD.ts2__name__c;
            ELSE 
                UPDATE jss_grouped_educations set count=(c-1) where name = OLD.ts2__name__c;
            END IF;

      END IF;

      IF(TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN
        
	        SELECT jss_grouped_educations.count into c from jss_grouped_educations where name = NEW.ts2__name__c limit 1;
          IF FOUND THEN
             UPDATE jss_grouped_educations set count=(c+1) where name = NEW.ts2__name__c;
          ELSE
             INSERT into jss_grouped_educations(count,name) values(1,NEW.ts2__name__c);
          END IF; 
	        
		  END IF;

        insert into jss_contact_jss_groupby_educations (jss_contact_id,jss_groupby_educations_id)
        select  c.id as jss_contact_id, geducation.id as jss_groupby_educations_id from ts2__education_history__c education 
        inner join contact c on education.ts2__contact__c = c.sfid 
        inner join jss_grouped_educations geducation on geducation.name = education.ts2__name__c
        where education.ts2__name__c = NEW.ts2__name__c and education.ts2__contact__c = NEW.ts2__contact__c 
        group by c.id, geducation.id;

      RETURN NEW;
  		EXCEPTION 
  			WHEN unique_violation THEN
  			raise notice 'error';
        	RETURN NEW;
      END;  
   $BODY$
    LANGUAGE plpgsql;

-- SCRIPTS
  DROP trigger if exists education_trigger_group on ts2__education_history__c;
 
-- SCRIPTS
  DROP trigger if exists education_trigger on ts2__education_history__c; 
  
-- SCRIPTS
CREATE  TRIGGER education_trigger
    AFTER INSERT OR UPDATE OF ts2__name__c,ts2__contact__c
    ON ts2__education_history__c
    FOR EACH ROW
    EXECUTE PROCEDURE update_educations_related_tables();

-- SCRIPTS
DROP FUNCTION if exists update_ex_group_employers() cascade;

-- SCRIPTS
DROP FUNCTION if exists update_jss_contact_groupby_employers() cascade;

-- SCRIPTS
CREATE OR REPLACE FUNCTION update_employers_related_tables() RETURNS trigger AS $BODY$ 
  DECLARE schema_name varchar; 
  DECLARE  c integer; 
  BEGIN
  schema_name := '"'||TG_TABLE_SCHEMA||'"';
  EXECUTE   'set search_path to '||schema_name;
      IF(TG_OP = 'UPDATE' OR TG_OP = 'DELETE') THEN

            delete from jss_contact_jss_groupby_employers t
            where t.jss_contact_id = (select id from contact where sfid = OLD.ts2__contact__c) 
            and t.jss_groupby_employers_id = (select id from jss_grouped_employers where name = OLD.ts2__name__c);
                       
            c:=(select "count" from jss_grouped_employers where name = OLD.ts2__name__c);
              
            IF (c=1) THEN 
                DELETE FROM  jss_grouped_employers where name = OLD.ts2__name__c;
            ELSE 
                UPDATE jss_grouped_employers set count=(c-1) where name = OLD.ts2__name__c;
            END IF;

      END IF;

      IF(TG_OP = 'UPDATE' OR TG_OP = 'INSERT') THEN
        
	        SELECT jss_grouped_employers.count into c from jss_grouped_employers where name = NEW.ts2__name__c limit 1;
          IF FOUND THEN
             UPDATE jss_grouped_employers set count=(c+1) where name = NEW.ts2__name__c;
          ELSE
             INSERT into jss_grouped_employers(count,name) values(1,NEW.ts2__name__c);
          END IF; 
	        
		  END IF;

        insert into jss_contact_jss_groupby_employers (jss_contact_id,jss_groupby_employers_id,year) 
        select  c.id as jss_contact_id, gemployer.id as jss_groupby_employers_id, max(EXTRACT(year from age(employer.ts2__employment_end_date__c,employer.ts2__employment_start_date__c))) 
        from ts2__employment_history__c employer 
        inner join contact c on employer.ts2__contact__c = c.sfid 
        inner join jss_grouped_employers gemployer on gemployer.name = employer.ts2__name__c 
        where employer.ts2__name__c = NEW.ts2__name__c and employer.ts2__contact__c = NEW.ts2__contact__c 
        group by c.id, gemployer.id;

      RETURN NEW;
  		EXCEPTION 
  			WHEN unique_violation THEN
  			raise notice 'error';
        	RETURN NEW;
      END;  
   $BODY$
    LANGUAGE plpgsql;

-- SCRIPTS
  DROP trigger if exists employer_trigger_group on ts2__employment_history__c;
   
-- SCRIPTS
  DROP trigger if exists employer_trigger on ts2__employment_history__c; 
  
-- SCRIPTS
CREATE  TRIGGER employer_trigger
    AFTER INSERT OR UPDATE OF ts2__name__c,ts2__contact__c
    ON ts2__employment_history__c
    FOR EACH ROW
    EXECUTE PROCEDURE update_employers_related_tables();


