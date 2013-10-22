-- SCRIPTS
CREATE TABLE if not exists contact_ex
(
  id bigint NOT NULL,
  resume_tsv tsvector,
  CONSTRAINT contact_ex_pkey PRIMARY KEY (id),
  CONSTRAINT fk_contact_ex_contact FOREIGN KEY (id)
      REFERENCES contact (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
);
    
-- SCRIPTS
	CREATE OR REPLACE FUNCTION update_context_ex_resume() RETURNS trigger AS $Body$
	BEGIN
	IF( TG_OP='INSERT' ) THEN
	    insert into contact_ex(id,resume_tsv) values(new.id,to_tsvector('english', new."ts2__text_resume__c" ));
	ELSIF (TG_OP = 'UPDATE') THEN
	    UPDATE contact_ex SET resume_tsv=to_tsvector('english', new."ts2__text_resume__c" )  where id = new.id;
	END IF;
	RETURN NEW;
	END;
	$Body$
	LANGUAGE 'plpgsql';
	
-- SCRIPTS
	DROP TRIGGER if exists contact_trg_resume_tsv ON contact  CASCADE;
-- SCRIPTS
CREATE TRIGGER contact_trg_resume_tsv
  BEFORE INSERT OR UPDATE OF "ts2__text_resume__c"
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
CREATE TABLE if not exists label
(
  id bigserial NOT NULL,
  user_id bigint,
  name character varying(128) NOT NULL,
  CONSTRAINT pk_label PRIMARY KEY (id),
  CONSTRAINT contact_id FOREIGN KEY (user_id)
      REFERENCES "user" (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE
);

-- SCRIPTS
CREATE TABLE if not exists label_contact
(
  label_id bigserial NOT NULL,
  contact_id bigint NOT NULL,
  CONSTRAINT primarykey PRIMARY KEY (label_id, contact_id),
  CONSTRAINT contact_id FOREIGN KEY (contact_id)
      REFERENCES contact (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT label_id FOREIGN KEY (label_id)
      REFERENCES label (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE
);