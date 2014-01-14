-- SCRIPTS
	CREATE TABLE if not exists contact_ex
	(
	  id bigint NOT NULL,
	  resume_tsv tsvector,
	  skills_tsv tsvector,
	  sfid character varying(18),
	  CONSTRAINT contact_ex_pkey PRIMARY KEY (id),
	  CONSTRAINT fk_contact_ex_contact FOREIGN KEY (id)
	      REFERENCES contact (id) MATCH SIMPLE
	      ON UPDATE NO ACTION ON DELETE CASCADE
	)
	WITH (
	  OIDS=FALSE
	);
    
-- SCRIPTS
	CREATE OR REPLACE FUNCTION update_context_ex_resume() RETURNS trigger AS $Body$
	BEGIN
	IF( TG_OP='INSERT' ) THEN
	    insert into contact_ex(id,resume_tsv,contact_tsv,sfid) values(new.id,to_tsvector('english', new."ts2__text_resume__c" ),to_tsvector('english',new."firstname"||' '||new."lastname"||' '||new."title"),new.sfid||' ');
	ELSIF (TG_OP = 'UPDATE') THEN
	    UPDATE contact_ex SET resume_tsv=to_tsvector('english', new."ts2__text_resume__c" ),contact_tsv= to_tsvector('english',new."firstname"||' '||new."lastname"||' '||new."title")  where id = new.id;
	END IF;
	RETURN NEW;
	END;
	$Body$
	LANGUAGE 'plpgsql';
	
-- SCRIPTS
	DROP TRIGGER if exists contact_trg_resume_tsv ON contact  CASCADE;
-- SCRIPTS
CREATE TRIGGER contact_trg_resume_tsv
  AFTER INSERT OR UPDATE OF "ts2__text_resume__c","firstname","lastname","sfid"
  ON contact
  FOR EACH ROW
  EXECUTE PROCEDURE update_context_ex_resume();
  
-- SCRIPTS
CREATE TABLE if not exists savedsearches
(
  id bigserial NOT NULL,
  user_id bigint,
  name character varying(64) NOT NULL,
  create_date timestamp ,
  update_date timestamp,
  search text NOT NULL,
  CONSTRAINT pk_savedsearched PRIMARY KEY (id),
  CONSTRAINT unq_name UNIQUE (name)
);


-- SCRIPTS
CREATE TABLE if not exists "user"
(
  id bigserial NOT NULL,
  sfid character varying(255) NULL,
  ctoken character varying(255) NOT NULL,
  CONSTRAINT pk_user PRIMARY KEY (id)
);


-- SCRIPTS
CREATE TABLE if not exists searchlog
(
  id bigserial NOT NULL,
  user_id bigint NOT NULL,
  date timestamp with time zone,
  search character varying(512),
  perfcount bigint NOT NULL DEFAULT 0,
  perffetch bigint NOT NULL DEFAULT 0,
  CONSTRAINT searchlog_pkey PRIMARY KEY (id)
);

-- SCRIPTS
CREATE OR REPLACE FUNCTION update_ex_group_skills() RETURNS trigger AS $BODY$ 
  DECLARE  c integer; 
    BEGIN 
      IF OLD.ts2__skill_name__c<>NEW.ts2__skill_name__c THEN
        IF(TG_OP = 'UPDATE') THEN
          
            c:=(select "count" from ex_grouped_skills where name = OLD.ts2__skill_name__c);
              
            IF (c=1) THEN 
                DELETE FROM  ex_grouped_skills where name = OLD.ts2__skill_name__c;
            ELSE 
                UPDATE ex_grouped_skills set count=(c-1) where name = OLD.ts2__skill_name__c;
            END IF;
                
        END IF;

        SELECT ex_grouped_skills.count into c from ex_grouped_skills where name = NEW.ts2__skill_name__c limit 1;
        IF FOUND THEN
           UPDATE ex_grouped_skills set count=(c+1) where name = NEW.ts2__skill_name__c;
        ELSE
           INSERT into ex_grouped_skills(count,name) values(1,NEW.ts2__skill_name__c);
        END IF; 

      END IF;

        RETURN NEW;
      END;  
   $BODY$
    LANGUAGE plpgsql;
    
-- SCRIPTS
  DROP trigger if exists skill_trigger on ts2__skill__c;
  
-- SCRIPTS
  CREATE  TRIGGER skill_trigger
    AFTER INSERT OR UPDATE OF ts2__skill_name__c
    ON ts2__skill__c
    FOR EACH ROW
    EXECUTE PROCEDURE update_ex_group_skills();

-- SCRIPTS
CREATE OR REPLACE FUNCTION update_ex_group_educations() RETURNS trigger AS $BODY$ 
  DECLARE  c integer; 
    BEGIN 
      IF OLD.ts2__name__c<>NEW.ts2__name__c THEN
        IF(TG_OP = 'UPDATE') THEN
          
            c:=(select "count" from ex_grouped_educations where name = OLD.ts2__name__c);
              
            IF (c=1) THEN 
                DELETE FROM  ex_grouped_educations where name = OLD.ts2__name__c;
            ELSE 
                UPDATE ex_grouped_educations set count=(c-1) where name = OLD.ts2__name__c;
            END IF;
                
        END IF;

        SELECT ex_grouped_educations.count into c from ex_grouped_educations where name = NEW.ts2__name__c limit 1;
        IF FOUND THEN
           UPDATE ex_grouped_educations set count=(c+1) where name = NEW.ts2__name__c;
        ELSE
           INSERT into ex_grouped_educations(count,name) values(1,NEW.ts2__name__c);
        END IF; 

      END IF;

        RETURN NEW;
      END;  
   $BODY$
    LANGUAGE plpgsql;
    
-- SCRIPTS
  DROP trigger if exists education_trigger on ts2__education_history__c;
  
-- SCRIPTS
  CREATE  TRIGGER education_trigger
    AFTER INSERT OR UPDATE OF ts2__name__c
    ON ts2__education_history__c
    FOR EACH ROW
    EXECUTE PROCEDURE update_ex_group_educations();

-- SCRIPTS
CREATE OR REPLACE FUNCTION update_ex_group_employers() RETURNS trigger AS $BODY$  
  DECLARE  c integer; 
    BEGIN 
      IF OLD.ts2__name__c<>NEW.ts2__name__c THEN
        IF(TG_OP = 'UPDATE') THEN
          
            c:=(select "count" from ex_grouped_employers where name = OLD.ts2__name__c);
              
            IF (c=1) THEN 
                DELETE FROM  ex_grouped_employers where name = OLD.ts2__name__c;
            ELSE 
                UPDATE ex_grouped_employers set count=(c-1) where name = OLD.ts2__name__c;
            END IF;
                
        END IF;

        SELECT ex_grouped_employers.count into c from ex_grouped_employers where name = NEW.ts2__name__c limit 1;
        IF FOUND THEN
           UPDATE ex_grouped_employers set count=(c+1) where name = NEW.ts2__name__c;
        ELSE
           INSERT into ex_grouped_employers(count,name) values(1,NEW.ts2__name__c);
        END IF; 

      END IF;

        RETURN NEW;
      END;  
   $BODY$
    LANGUAGE plpgsql;
    
-- SCRIPTS
  DROP trigger if exists employer_trigger on ts2__employment_history__c;
  
-- SCRIPTS
  CREATE  TRIGGER employer_trigger
    AFTER INSERT OR UPDATE OF ts2__name__c
    ON ts2__employment_history__c
    FOR EACH ROW
    EXECUTE PROCEDURE update_ex_group_employers();


